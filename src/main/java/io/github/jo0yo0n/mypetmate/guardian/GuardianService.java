package io.github.jo0yo0n.mypetmate.guardian;

import io.github.jo0yo0n.mypetmate.auth.exception.AccountRestrictedException;
import io.github.jo0yo0n.mypetmate.auth.exception.AccountWithdrawnException;
import io.github.jo0yo0n.mypetmate.auth.exception.InvalidActivityRegionException;
import io.github.jo0yo0n.mypetmate.error.StateConflictException;
import io.github.jo0yo0n.mypetmate.error.UnexpectedFailureException;
import io.github.jo0yo0n.mypetmate.guardian.domain.ActivityTimeSlot;
import io.github.jo0yo0n.mypetmate.guardian.domain.GuardianStatus;
import io.github.jo0yo0n.mypetmate.guardian.dto.GuardianResponse;
import io.github.jo0yo0n.mypetmate.guardian.persistence.entity.Guardian;
import io.github.jo0yo0n.mypetmate.guardian.persistence.entity.GuardianActivityRegion;
import io.github.jo0yo0n.mypetmate.guardian.persistence.entity.GuardianAvailableTimeSlot;
import io.github.jo0yo0n.mypetmate.guardian.persistence.repository.GuardianActivityRegionRepository;
import io.github.jo0yo0n.mypetmate.guardian.persistence.repository.GuardianAvailableTimeSlotRepository;
import io.github.jo0yo0n.mypetmate.guardian.persistence.repository.GuardianRepository;
import io.github.jo0yo0n.mypetmate.guardian.persistence.repository.RefreshTokenRepository;
import io.github.jo0yo0n.mypetmate.region.persistence.entity.Region;
import io.github.jo0yo0n.mypetmate.region.persistence.repository.RegionRepository;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
class GuardianService {
  private static final String FAILURE_MESSAGE = "요청 처리 중 인증된 보호자를 찾을 수 없습니다.";
  private final GuardianRepository guardianRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final Clock clock;
  private final GuardianActivityRegionRepository activityRegionRepository;
  private final GuardianAvailableTimeSlotRepository timeSlotRepository;
  private final RegionRepository regionRepository;

  GuardianService(
      GuardianRepository guardianRepository,
      RefreshTokenRepository refreshTokenRepository,
      Clock clock,
      GuardianActivityRegionRepository activityRegionRepository,
      GuardianAvailableTimeSlotRepository timeSlotRepository,
      RegionRepository regionRepository) {
    this.guardianRepository = guardianRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.clock = clock;
    this.activityRegionRepository = activityRegionRepository;
    this.timeSlotRepository = timeSlotRepository;
    this.regionRepository = regionRepository;
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  GuardianResponse getCurrentGuardian(UUID guardianId) {
    Guardian guardian =
        guardianRepository
            .findById(guardianId)
            .orElseThrow(() -> new UnexpectedFailureException(FAILURE_MESSAGE));
    requireActive(guardian);
    return toResponse(guardian);
  }

  @Transactional
  GuardianResponse updateCurrentGuardian(UUID guardianId, GuardianUpdateCommand command) {
    Guardian guardian =
        guardianRepository
            .findByIdWithPessimisticWriteLock(guardianId)
            .orElseThrow(() -> new UnexpectedFailureException(FAILURE_MESSAGE));
    requireActive(guardian);
    guardian.updateProfile(
        command.nickname(),
        command.profileType(),
        command.gender(),
        command.discoveryStatus(),
        clock.instant());
    flushProfileUpdate();

    var regions = updateActivityRegions(guardian, command.activityRegionIds());
    var slots = updateAvailableTimeSlots(guardian, command.availableTimeSlots());
    return GuardianResponse.from(guardian, regions, slots);
  }

  @Transactional
  void withdrawCurrentGuardian(UUID guardianId) {
    Guardian guardian =
        guardianRepository
            .findByIdWithPessimisticWriteLock(guardianId)
            .orElseThrow(() -> new UnexpectedFailureException(FAILURE_MESSAGE));
    requireActive(guardian);
    var now = clock.instant();
    guardian.withdraw(now);
    refreshTokenRepository.revokeAllByGuardianId(guardianId, now);
  }

  private GuardianResponse toResponse(Guardian guardian) {
    var regions =
        activityRegionRepository.findAllByGuardianId(guardian.getId()).stream()
            .map(GuardianActivityRegion::getRegion)
            .toList();
    return GuardianResponse.from(
        guardian, regions, timeSlotRepository.findAllByGuardianId(guardian.getId()));
  }

  private static void requireActive(Guardian guardian) {
    if (guardian.getStatus() == GuardianStatus.WITHDRAWN) throw new AccountWithdrawnException();
    if (guardian.getStatus() == GuardianStatus.TEMPORARILY_RESTRICTED)
      throw new AccountRestrictedException();
  }

  private void flushProfileUpdate() {
    try {
      guardianRepository.flush();
    } catch (DataIntegrityViolationException exception) {
      for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
        if (cause instanceof ConstraintViolationException violation
            && "uk_guardian_nickname".equals(violation.getConstraintName())) {
          throw new StateConflictException("이미 사용 중인 닉네임입니다.");
        }
      }
      throw exception;
    }
  }

  private List<Region> updateActivityRegions(Guardian guardian, List<UUID> requestedIds) {
    var existing = activityRegionRepository.findAllByGuardianId(guardian.getId());
    if (requestedIds == null || requestedIds.isEmpty()) {
      return existing.stream().map(GuardianActivityRegion::getRegion).toList();
    }

    var desiredIds = new HashSet<>(requestedIds);
    var desiredRegions = regionRepository.findSelectableLocalitiesByIdIn(requestedIds);
    if (desiredRegions.size() != requestedIds.size()) {
      throw new InvalidActivityRegionException(requestedIds, desiredRegions);
    }

    var existingIds =
        existing.stream().map(link -> link.getRegion().getId()).collect(Collectors.toSet());
    activityRegionRepository.deleteAll(
        existing.stream().filter(link -> !desiredIds.contains(link.getRegion().getId())).toList());
    activityRegionRepository.saveAll(
        desiredRegions.stream()
            .filter(region -> !existingIds.contains(region.getId()))
            .map(region -> new GuardianActivityRegion(UUID.randomUUID(), guardian, region))
            .toList());
    return desiredRegions;
  }

  private List<GuardianAvailableTimeSlot> updateAvailableTimeSlots(
      Guardian guardian, List<ActivityTimeSlot> desired) {
    var existing = timeSlotRepository.findAllByGuardianId(guardian.getId());
    if (desired == null || desired.isEmpty()) {
      return existing;
    }

    var desiredKeys = new HashSet<>(desired);
    var existingByKey =
        existing.stream()
            .collect(Collectors.toMap(GuardianService::toActivityTimeSlot, slot -> slot));
    timeSlotRepository.deleteAll(
        existing.stream().filter(slot -> !desiredKeys.contains(toActivityTimeSlot(slot))).toList());
    var added =
        timeSlotRepository.saveAll(
            desired.stream()
                .filter(slot -> !existingByKey.containsKey(slot))
                .map(
                    slot ->
                        new GuardianAvailableTimeSlot(
                            UUID.randomUUID(),
                            guardian,
                            slot.dayOfWeek(),
                            slot.startMinute(),
                            slot.endMinute()))
                .toList());
    added.forEach(slot -> existingByKey.put(toActivityTimeSlot(slot), slot));
    return desired.stream().map(existingByKey::get).toList();
  }

  private static ActivityTimeSlot toActivityTimeSlot(GuardianAvailableTimeSlot slot) {
    return new ActivityTimeSlot(slot.getDayOfWeek(), slot.getStartMinute(), slot.getEndMinute());
  }
}

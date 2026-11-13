package io.github.jo0yo0n.mypetmate.guardian;

import io.github.jo0yo0n.mypetmate.auth.exception.AccountRestrictedException;
import io.github.jo0yo0n.mypetmate.auth.exception.AccountWithdrawnException;
import io.github.jo0yo0n.mypetmate.error.UnexpectedFailureException;
import io.github.jo0yo0n.mypetmate.guardian.domain.GuardianStatus;
import io.github.jo0yo0n.mypetmate.guardian.dto.GuardianResponse;
import io.github.jo0yo0n.mypetmate.guardian.dto.GuardianUpdateRequest;
import io.github.jo0yo0n.mypetmate.guardian.persistence.entity.Guardian;
import io.github.jo0yo0n.mypetmate.guardian.persistence.entity.GuardianActivityRegion;
import io.github.jo0yo0n.mypetmate.guardian.persistence.repository.GuardianActivityRegionRepository;
import io.github.jo0yo0n.mypetmate.guardian.persistence.repository.GuardianAvailableTimeSlotRepository;
import io.github.jo0yo0n.mypetmate.guardian.persistence.repository.GuardianRepository;
import io.github.jo0yo0n.mypetmate.guardian.persistence.repository.RefreshTokenRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class GuardianService {
  private static final String FAILURE_MESSAGE = "요청 처리 중 인증된 보호자를 찾을 수 없습니다.";
  private final GuardianRepository guardianRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final Clock clock;
  private final GuardianActivityRegionRepository activityRegionRepository;
  private final GuardianAvailableTimeSlotRepository timeSlotRepository;

  GuardianService(
      GuardianRepository guardianRepository,
      RefreshTokenRepository refreshTokenRepository,
      Clock clock,
      GuardianActivityRegionRepository activityRegionRepository,
      GuardianAvailableTimeSlotRepository timeSlotRepository) {
    this.guardianRepository = guardianRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.clock = clock;
    this.activityRegionRepository = activityRegionRepository;
    this.timeSlotRepository = timeSlotRepository;
  }

  @Transactional(readOnly = true)
  GuardianResponse getCurrentGuardian(UUID guardianId) {
    Guardian guardian =
        guardianRepository
            .findById(guardianId)
            .orElseThrow(() -> new UnexpectedFailureException(FAILURE_MESSAGE));
    requireActive(guardian);
    return toResponse(guardian);
  }

  @Transactional
  GuardianResponse updateCurrentGuardian(UUID guardianId, GuardianUpdateRequest request) {
    Guardian guardian =
        guardianRepository
            .findByIdWithPessimisticWriteLock(guardianId)
            .orElseThrow(() -> new UnexpectedFailureException(FAILURE_MESSAGE));
    requireActive(guardian);
    guardian.updateProfile(
        request.profileType(), request.gender(), request.discoveryStatus(), clock.instant());
    return toResponse(guardian);
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
}

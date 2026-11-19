package io.github.jo0yo0n.mypetmate.auth;

import io.github.jo0yo0n.mypetmate.auth.dto.AuthResponse;
import io.github.jo0yo0n.mypetmate.auth.dto.LoginRequest;
import io.github.jo0yo0n.mypetmate.auth.dto.RefreshRequest;
import io.github.jo0yo0n.mypetmate.auth.dto.TokenResponse;
import io.github.jo0yo0n.mypetmate.auth.exception.AccountRestrictedException;
import io.github.jo0yo0n.mypetmate.auth.exception.AccountWithdrawnException;
import io.github.jo0yo0n.mypetmate.auth.exception.EmailAlreadyExistsException;
import io.github.jo0yo0n.mypetmate.auth.exception.InvalidActivityRegionException;
import io.github.jo0yo0n.mypetmate.auth.exception.InvalidCredentialsException;
import io.github.jo0yo0n.mypetmate.auth.exception.InvalidRefreshTokenException;
import io.github.jo0yo0n.mypetmate.auth.exception.NicknameAlreadyExistsException;
import io.github.jo0yo0n.mypetmate.config.TokenProperties;
import io.github.jo0yo0n.mypetmate.guardian.domain.ActivityTimeSlot;
import io.github.jo0yo0n.mypetmate.guardian.domain.GuardianStatus;
import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import io.github.jo0yo0n.mypetmate.guardian.dto.GuardianResponse;
import io.github.jo0yo0n.mypetmate.guardian.persistence.entity.Guardian;
import io.github.jo0yo0n.mypetmate.guardian.persistence.entity.GuardianActivityRegion;
import io.github.jo0yo0n.mypetmate.guardian.persistence.entity.GuardianAvailableTimeSlot;
import io.github.jo0yo0n.mypetmate.guardian.persistence.entity.RefreshToken;
import io.github.jo0yo0n.mypetmate.guardian.persistence.repository.GuardianActivityRegionRepository;
import io.github.jo0yo0n.mypetmate.guardian.persistence.repository.GuardianAvailableTimeSlotRepository;
import io.github.jo0yo0n.mypetmate.guardian.persistence.repository.GuardianRepository;
import io.github.jo0yo0n.mypetmate.guardian.persistence.repository.RefreshTokenRepository;
import io.github.jo0yo0n.mypetmate.region.persistence.entity.Region;
import io.github.jo0yo0n.mypetmate.region.persistence.repository.RegionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final Clock clock;
  private final AccessTokenIssuer accessTokenIssuer;
  private final TokenProperties tokenProperties;
  private final GuardianRepository guardianRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final RefreshTokenGenerator refreshTokenGenerator;
  private final GuardianActivityRegionRepository guardianActivityRegionRepository;
  private final GuardianAvailableTimeSlotRepository guardianAvailableTimeSlotRepository;
  private final RegionRepository regionRepository;
  private final PasswordEncoder passwordEncoder;
  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  public AuthService(
      Clock clock,
      AccessTokenIssuer accessTokenIssuer,
      TokenProperties tokenProperties,
      GuardianRepository guardianRepository,
      RefreshTokenRepository refreshTokenRepository,
      RefreshTokenGenerator refreshTokenGenerator,
      GuardianActivityRegionRepository guardianActivityRegionRepository,
      GuardianAvailableTimeSlotRepository guardianAvailableTimeSlotRepository,
      RegionRepository regionRepository,
      PasswordEncoder passwordEncoder) {

    this.clock = clock;
    this.accessTokenIssuer = accessTokenIssuer;
    this.tokenProperties = tokenProperties;
    this.guardianRepository = guardianRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.refreshTokenGenerator = refreshTokenGenerator;
    this.guardianActivityRegionRepository = guardianActivityRegionRepository;
    this.guardianAvailableTimeSlotRepository = guardianAvailableTimeSlotRepository;
    this.regionRepository = regionRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  AuthResponse signup(SignupCommand command) {

    Instant now = clock.instant();
    Guardian guardian;
    try {
      guardian =
          guardianRepository.saveAndFlush(
              new Guardian(
                  UUID.randomUUID(),
                  command.nickname(),
                  command.email(),
                  passwordEncoder.encode(command.password()),
                  command.profileType(),
                  command.profileType() == ProfileType.INDIVIDUAL ? command.gender() : null,
                  command.discoveryStatus(),
                  GuardianStatus.ACTIVE,
                  now,
                  now));
    } catch (DataIntegrityViolationException exception) {
      Throwable cause = exception;

      while (cause != null) {
        if (cause instanceof ConstraintViolationException violation) {
          String constraintName = violation.getConstraintName();
          if ("uk_guardian_email_lower".equals(constraintName)) {
            throw new EmailAlreadyExistsException();
          }
          if ("uk_guardian_nickname".equals(constraintName)) {
            throw new NicknameAlreadyExistsException();
          }
        }
        cause = cause.getCause();
      }
      throw exception;
    }

    List<UUID> requestedRegionIds =
        Objects.requireNonNullElse(command.activityRegionIds(), List.of());

    List<Region> regions =
        requestedRegionIds.isEmpty()
            ? List.of()
            : regionRepository.findSelectableUmdsByIdIn(requestedRegionIds);

    if (regions.size() != requestedRegionIds.size()) {
      throw new InvalidActivityRegionException(requestedRegionIds, regions);
    }

    if (!regions.isEmpty()) {
      guardianActivityRegionRepository.saveAll(
          regions.stream()
              .map(region -> new GuardianActivityRegion(UUID.randomUUID(), guardian, region))
              .toList());
    }

    List<ActivityTimeSlot> requestedSlots = command.availableTimeSlots();
    List<GuardianAvailableTimeSlot> slots =
        requestedSlots == null
            ? List.of()
            : requestedSlots.stream()
                .map(
                    slot ->
                        new GuardianAvailableTimeSlot(
                            UUID.randomUUID(),
                            guardian,
                            slot.dayOfWeek(),
                            slot.startMinute(),
                            slot.endMinute()))
                .toList();
    if (!slots.isEmpty()) {
      guardianAvailableTimeSlotRepository.saveAll(slots);
    }

    AuthResponse authResponse =
        makeAuthResponse(guardian, now, GuardianResponse.from(guardian, regions, slots));
    log.info("signup processing completed");
    return authResponse;
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ)
  AuthResponse login(LoginRequest loginRequest) {

    Instant now = clock.instant();
    Guardian guardian =
        guardianRepository
            .findByEmail(loginRequest.email())
            .orElseThrow(InvalidCredentialsException::new);

    if (!passwordEncoder.matches(loginRequest.password(), guardian.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }

    if (guardian.getStatus() == GuardianStatus.WITHDRAWN) {
      throw new AccountWithdrawnException();
    } else if (guardian.getStatus() == GuardianStatus.TEMPORARILY_RESTRICTED) {
      throw new AccountRestrictedException();
    }

    var regions =
        guardianActivityRegionRepository.findAllByGuardianId(guardian.getId()).stream()
            .map(GuardianActivityRegion::getRegion)
            .toList();

    AuthResponse authResponse =
        makeAuthResponse(
            guardian,
            now,
            GuardianResponse.from(
                guardian,
                regions,
                guardianAvailableTimeSlotRepository.findAllByGuardianId(guardian.getId())));
    log.info("login processing completed");
    return authResponse;
  }

  @Transactional
  TokenResponse refresh(RefreshRequest refreshRequest) {

    Instant now = clock.instant();

    UUID guardianId =
        refreshTokenRepository
            .findGuardianIdByTokenHash(refreshTokenGenerator.hash(refreshRequest.refreshToken()))
            .orElseThrow(InvalidRefreshTokenException::new);
    Guardian guardian =
        guardianRepository
            .findByIdWithPessimisticWriteLock(guardianId)
            .orElseThrow(InvalidRefreshTokenException::new);
    RefreshToken legacyRefreshToken =
        refreshTokenRepository
            .findByTokenHashWithPessimisticWriteLock(
                refreshTokenGenerator.hash(refreshRequest.refreshToken()))
            .orElseThrow(InvalidRefreshTokenException::new);

    if (!legacyRefreshToken.getExpiresAt().isAfter(now)
        || legacyRefreshToken.getRevokedAt() != null) {

      throw new InvalidRefreshTokenException();
    }

    if (guardian.getStatus() == GuardianStatus.WITHDRAWN) {
      throw new AccountWithdrawnException();
    } else if (guardian.getStatus() == GuardianStatus.TEMPORARILY_RESTRICTED) {
      throw new AccountRestrictedException();
    }

    legacyRefreshToken.setRevokedAt(clock.instant());

    String accessToken = accessTokenIssuer.issue(guardian, now);
    String refreshToken = generateAndSaveRefreshToken(guardian, now);

    log.info("refresh processing completed");
    return new TokenResponse(
        accessToken,
        refreshToken,
        tokenProperties.tokenType(),
        Math.toIntExact(tokenProperties.accessTokenTtl().toSeconds()),
        Math.toIntExact(tokenProperties.refreshTokenTtl().toSeconds()));
  }

  @Transactional
  void logout(RefreshRequest logoutRequest) {

    Instant now = clock.instant();
    refreshTokenRepository
        .findByTokenHash(refreshTokenGenerator.hash(logoutRequest.refreshToken()))
        .ifPresent(
            refreshToken -> {
              if (!refreshToken.getExpiresAt().isAfter(now)) {
                refreshTokenRepository.deleteByTokenHash(refreshToken.getTokenHash());
                return;
              }
              if (refreshToken.getRevokedAt() == null) {
                refreshToken.setRevokedAt(now);
              }
            });
  }

  private AuthResponse makeAuthResponse(Guardian guardian, Instant now, GuardianResponse profile) {

    String refreshToken = generateAndSaveRefreshToken(guardian, now);
    String accessToken = accessTokenIssuer.issue(guardian, now);

    return new AuthResponse(
        accessToken,
        refreshToken,
        tokenProperties.tokenType(),
        Math.toIntExact(tokenProperties.accessTokenTtl().toSeconds()),
        Math.toIntExact(tokenProperties.refreshTokenTtl().toSeconds()),
        profile);
  }

  private String generateAndSaveRefreshToken(Guardian guardian, Instant now) {
    String refreshToken = refreshTokenGenerator.generate();
    refreshTokenRepository.save(
        new RefreshToken(
            UUID.randomUUID(),
            guardian,
            refreshTokenGenerator.hash(refreshToken),
            now.plus(tokenProperties.refreshTokenTtl()),
            null,
            now));

    return refreshToken;
  }
}

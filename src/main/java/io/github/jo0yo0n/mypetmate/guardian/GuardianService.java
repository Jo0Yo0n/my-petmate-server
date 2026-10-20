package io.github.jo0yo0n.mypetmate.guardian;

import io.github.jo0yo0n.mypetmate.auth.exception.AccountRestrictedException;
import io.github.jo0yo0n.mypetmate.auth.exception.AccountWithdrawnException;
import io.github.jo0yo0n.mypetmate.error.UnexpectedFailureException;
import io.github.jo0yo0n.mypetmate.guardian.domain.GuardianStatus;
import io.github.jo0yo0n.mypetmate.guardian.dto.GuardianResponse;
import io.github.jo0yo0n.mypetmate.guardian.dto.GuardianUpdateRequest;
import io.github.jo0yo0n.mypetmate.guardian.persistence.Guardian;
import io.github.jo0yo0n.mypetmate.guardian.persistence.GuardianRepository;
import io.github.jo0yo0n.mypetmate.guardian.persistence.RefreshTokenRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class GuardianService {
  private final GuardianRepository guardianRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final Clock clock;

  GuardianService(
      GuardianRepository guardianRepository,
      RefreshTokenRepository refreshTokenRepository,
      Clock clock) {
    this.guardianRepository = guardianRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  GuardianResponse getCurrentGuardian(UUID guardianId) {
    Guardian guardian =
        guardianRepository
            .findById(guardianId)
            .orElseThrow(
                () ->
                    new UnexpectedFailureException(
                        "Authenticated guardian was not found during service processing"));
    requireActive(guardian);
    return GuardianResponse.from(guardian);
  }

  @Transactional
  GuardianResponse updateCurrentGuardian(UUID guardianId, GuardianUpdateRequest request) {
    Guardian guardian =
        guardianRepository
            .findByIdWithPessimisticWriteLock(guardianId)
            .orElseThrow(
                () ->
                    new UnexpectedFailureException(
                        "Authenticated guardian was not found during service processing"));
    requireActive(guardian);
    guardian.updateProfile(
        request.profileType(), request.gender(), request.identityVisibility(), clock.instant());
    return GuardianResponse.from(guardian);
  }

  @Transactional
  void withdrawCurrentGuardian(UUID guardianId) {
    Guardian guardian =
        guardianRepository
            .findByIdWithPessimisticWriteLock(guardianId)
            .orElseThrow(
                () ->
                    new UnexpectedFailureException(
                        "Authenticated guardian was not found during service processing"));
    requireActive(guardian);
    var now = clock.instant();
    guardian.withdraw(now);
    refreshTokenRepository.revokeAllByGuardianId(guardianId, now);
  }

  private static void requireActive(Guardian guardian) {
    if (guardian.getStatus() == GuardianStatus.WITHDRAWN) throw new AccountWithdrawnException();
    if (guardian.getStatus() == GuardianStatus.TEMPORARILY_RESTRICTED)
      throw new AccountRestrictedException();
  }
}

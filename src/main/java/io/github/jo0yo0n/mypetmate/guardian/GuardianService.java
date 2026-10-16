package io.github.jo0yo0n.mypetmate.guardian;

import io.github.jo0yo0n.mypetmate.auth.exception.AccountRestrictedException;
import io.github.jo0yo0n.mypetmate.auth.exception.AccountWithdrawnException;
import io.github.jo0yo0n.mypetmate.auth.exception.InvalidAccessTokenException;
import io.github.jo0yo0n.mypetmate.guardian.domain.GuardianStatus;
import io.github.jo0yo0n.mypetmate.guardian.dto.GuardianResponse;
import io.github.jo0yo0n.mypetmate.guardian.dto.GuardianUpdateRequest;
import io.github.jo0yo0n.mypetmate.guardian.persistence.Guardian;
import io.github.jo0yo0n.mypetmate.guardian.persistence.GuardianRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class GuardianService {
  private final GuardianRepository guardianRepository;
  private final Clock clock;

  GuardianService(GuardianRepository guardianRepository, Clock clock) {
    this.guardianRepository = guardianRepository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  GuardianResponse getCurrentGuardian(UUID guardianId) {
    Guardian guardian =
        guardianRepository.findById(guardianId).orElseThrow(InvalidAccessTokenException::new);
    requireActive(guardian);
    return GuardianResponse.from(guardian);
  }

  @Transactional
  GuardianResponse updateCurrentGuardian(UUID guardianId, GuardianUpdateRequest request) {
    Guardian guardian =
        guardianRepository
            .findByIdWithPessimisticWriteLock(guardianId)
            .orElseThrow(InvalidAccessTokenException::new);
    requireActive(guardian);
    guardian.updateProfile(
        request.profileType(), request.gender(), request.identityVisibility(), clock.instant());
    return GuardianResponse.from(guardian);
  }

  private static void requireActive(Guardian guardian) {
    if (guardian.getStatus() == GuardianStatus.WITHDRAWN) throw new AccountWithdrawnException();
    if (guardian.getStatus() == GuardianStatus.TEMPORARILY_RESTRICTED)
      throw new AccountRestrictedException();
  }
}

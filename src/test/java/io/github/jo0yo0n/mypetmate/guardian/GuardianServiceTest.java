package io.github.jo0yo0n.mypetmate.guardian;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.jo0yo0n.mypetmate.error.UnexpectedFailureException;
import io.github.jo0yo0n.mypetmate.guardian.domain.DiscoveryStatus;
import io.github.jo0yo0n.mypetmate.guardian.domain.Gender;
import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import io.github.jo0yo0n.mypetmate.guardian.dto.GuardianUpdateRequest;
import io.github.jo0yo0n.mypetmate.guardian.persistence.GuardianRepository;
import io.github.jo0yo0n.mypetmate.guardian.persistence.RefreshTokenRepository;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuardianServiceTest {
  private static final String FAILURE_MESSAGE =
      "Authenticated guardian was not found during service processing";

  @Mock GuardianRepository guardianRepository;
  @Mock RefreshTokenRepository refreshTokenRepository;
  @Mock Clock clock;

  private final UUID guardianId = UUID.randomUUID();
  private GuardianService guardianService;

  @BeforeEach
  void setUp() {
    guardianService = new GuardianService(guardianRepository, refreshTokenRepository, clock);
  }

  @Test
  @DisplayName("[M1-API-ME-10] returnsUnexpectedFailureWhenCurrentGuardianIsMissing")
  void returnsUnexpectedFailureWhenCurrentGuardianIsMissing() {
    given(guardianRepository.findById(guardianId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> guardianService.getCurrentGuardian(guardianId))
        .isInstanceOf(UnexpectedFailureException.class)
        .hasMessage(FAILURE_MESSAGE);
    verifyNoInteractions(refreshTokenRepository, clock);
  }

  @Test
  @DisplayName("[M1-API-ME-10] returnsUnexpectedFailureWhenGuardianIsMissingDuringUpdate")
  void returnsUnexpectedFailureWhenGuardianIsMissingDuringUpdate() {
    given(guardianRepository.findByIdWithPessimisticWriteLock(guardianId))
        .willReturn(Optional.empty());
    var request =
        new GuardianUpdateRequest(ProfileType.INDIVIDUAL, Gender.FEMALE, DiscoveryStatus.SEARCHING);

    assertThatThrownBy(() -> guardianService.updateCurrentGuardian(guardianId, request))
        .isInstanceOf(UnexpectedFailureException.class)
        .hasMessage(FAILURE_MESSAGE);
    verifyNoInteractions(refreshTokenRepository, clock);
  }

  @Test
  @DisplayName("[M1-API-WD-08] returnsUnexpectedFailureWhenGuardianIsMissingDuringWithdrawal")
  void returnsUnexpectedFailureWhenGuardianIsMissingDuringWithdrawal() {
    given(guardianRepository.findByIdWithPessimisticWriteLock(guardianId))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> guardianService.withdrawCurrentGuardian(guardianId))
        .isInstanceOf(UnexpectedFailureException.class)
        .hasMessage(FAILURE_MESSAGE);
    verifyNoInteractions(refreshTokenRepository, clock);
  }
}

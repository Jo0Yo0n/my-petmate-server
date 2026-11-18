package io.github.jo0yo0n.mypetmate.guardian;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.jo0yo0n.mypetmate.error.UnexpectedFailureException;
import io.github.jo0yo0n.mypetmate.guardian.domain.DiscoveryStatus;
import io.github.jo0yo0n.mypetmate.guardian.domain.Gender;
import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import io.github.jo0yo0n.mypetmate.guardian.persistence.repository.GuardianActivityRegionRepository;
import io.github.jo0yo0n.mypetmate.guardian.persistence.repository.GuardianAvailableTimeSlotRepository;
import io.github.jo0yo0n.mypetmate.guardian.persistence.repository.GuardianRepository;
import io.github.jo0yo0n.mypetmate.guardian.persistence.repository.RefreshTokenRepository;
import io.github.jo0yo0n.mypetmate.region.persistence.repository.RegionRepository;
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
  private static final String FAILURE_MESSAGE = "요청 처리 중 인증된 보호자를 찾을 수 없습니다.";

  @Mock GuardianRepository guardianRepository;
  @Mock RefreshTokenRepository refreshTokenRepository;
  @Mock Clock clock;
  @Mock GuardianActivityRegionRepository activityRegionRepository;
  @Mock GuardianAvailableTimeSlotRepository timeSlotRepository;
  @Mock RegionRepository regionRepository;

  private final UUID guardianId = UUID.randomUUID();
  private GuardianService guardianService;

  @BeforeEach
  void setUp() {
    guardianService =
        new GuardianService(
            guardianRepository,
            refreshTokenRepository,
            clock,
            activityRegionRepository,
            timeSlotRepository,
            regionRepository);
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
        new GuardianUpdateCommand(
            "보호자", ProfileType.INDIVIDUAL, Gender.FEMALE, DiscoveryStatus.PAUSED, null, null);

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

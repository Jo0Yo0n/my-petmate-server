package io.github.jo0yo0n.mypetmate.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import io.github.jo0yo0n.mypetmate.auth.dto.LoginRequest;
import io.github.jo0yo0n.mypetmate.guardian.domain.ActivityTimeSlot;
import io.github.jo0yo0n.mypetmate.guardian.domain.DiscoveryStatus;
import io.github.jo0yo0n.mypetmate.guardian.domain.Gender;
import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import io.github.jo0yo0n.mypetmate.guardian.domain.Weekday;
import io.github.jo0yo0n.mypetmate.guardian.dto.GuardianResponse;
import io.github.jo0yo0n.mypetmate.guardian.persistence.entity.RefreshToken;
import io.github.jo0yo0n.mypetmate.guardian.persistence.repository.GuardianAvailableTimeSlotRepository;
import io.github.jo0yo0n.mypetmate.guardian.persistence.repository.GuardianRepository;
import io.github.jo0yo0n.mypetmate.guardian.persistence.repository.RefreshTokenRepository;
import io.github.jo0yo0n.mypetmate.region.domain.RegionLevel;
import io.github.jo0yo0n.mypetmate.support.PostgreSqlIntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;

@Sql(PostgreSqlIntegrationTestSupport.ANSAN_REGIONS_SQL)
@SpringBootTest
public class AuthServiceSignupRollbackTest extends PostgreSqlIntegrationTestSupport {

  @Autowired private AuthService authService;
  @Autowired private GuardianRepository guardianRepository;
  @Autowired private GuardianAvailableTimeSlotRepository slotRepository;
  @MockitoSpyBean private RefreshTokenRepository refreshTokenRepository;

  @DisplayName("[M1-AUTH-03] rollbackWhenTransactionFailed")
  @Test
  void rollbackWhenTransactionFailed() {

    // given
    doThrow(DataIntegrityViolationException.class)
        .when(refreshTokenRepository)
        .save(any(RefreshToken.class));

    SignupCommand signupRequest =
        new SignupCommand(
            "보호자",
            "guardian@example.com",
            "StrongPassword1!",
            ProfileType.INDIVIDUAL,
            Gender.MALE,
            DiscoveryStatus.PAUSED,
            null,
            List.of(new ActivityTimeSlot(Weekday.MONDAY, 540, 630)));

    assertThat(guardianRepository.count()).isEqualTo(0);
    assertThat(refreshTokenRepository.count()).isEqualTo(0);

    // when & then
    assertThatThrownBy(() -> authService.signup(signupRequest))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(guardianRepository.findByEmail("guardian@example.com")).isEmpty();
    assertThat(guardianRepository.count()).isEqualTo(0);
    assertThat(refreshTokenRepository.count()).isEqualTo(0);
    assertThat(slotRepository.count()).isZero();
  }

  @Test
  void returnsStoredProfileAtSignupAndLogin() {
    var request =
        new SignupCommand(
            "보호자",
            "guardian@example.com",
            "StrongPassword1!",
            ProfileType.INDIVIDUAL,
            Gender.MALE,
            DiscoveryStatus.PAUSED,
            List.of(IL_DONG_ID),
            List.of(new ActivityTimeSlot(Weekday.MONDAY, 1430, 1440)));

    var signupProfile = authService.signup(request).guardian();
    assertThat(signupProfile.nickname()).isEqualTo("보호자");
    assertThat(signupProfile.photoUrl()).isNull();
    assertThat(signupProfile.activityRegions())
        .containsExactly(
            new GuardianResponse.ActivityRegion(
                IL_DONG_ID,
                List.of(
                    new GuardianResponse.RegionPathSegment(RegionLevel.PROVINCE, "경기도"),
                    new GuardianResponse.RegionPathSegment(RegionLevel.DISTRICT, "안산시 상록구"),
                    new GuardianResponse.RegionPathSegment(RegionLevel.LOCALITY, "일동"))));
    assertThat(signupProfile.availableTimeSlots())
        .containsExactly(new GuardianResponse.AvailableTimeSlot(Weekday.MONDAY, "23:50", "24:00"));
    assertThat(authService.login(new LoginRequest(request.email(), request.password())).guardian())
        .isEqualTo(signupProfile);
    var guardian = guardianRepository.findByEmail(request.email()).orElseThrow();

    assertThat(slotRepository.findAll())
        .singleElement()
        .satisfies(
            slot -> {
              assertThat(slot.getGuardian().getId()).isEqualTo(guardian.getId());
              assertThat(slot.getDayOfWeek()).isEqualTo(Weekday.MONDAY);
              assertThat(slot.getStartMinute()).isEqualTo(1430);
              assertThat(slot.getEndMinute()).isEqualTo(1440);
            });
  }
}

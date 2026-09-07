package io.github.jo0yo0n.mypetmate.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import io.github.jo0yo0n.mypetmate.guardian.domain.Gender;
import io.github.jo0yo0n.mypetmate.guardian.domain.IdentityVisibility;
import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import io.github.jo0yo0n.mypetmate.guardian.dto.SignupRequest;
import io.github.jo0yo0n.mypetmate.guardian.persistence.GuardianRepository;
import io.github.jo0yo0n.mypetmate.guardian.persistence.RefreshToken;
import io.github.jo0yo0n.mypetmate.guardian.persistence.RefreshTokenRepository;
import io.github.jo0yo0n.mypetmate.support.PostgreSqlIntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
public class AuthServiceSignupRollbackTest extends PostgreSqlIntegrationTestSupport {

  @Autowired private AuthService authService;
  @Autowired private GuardianRepository guardianRepository;
  @MockitoSpyBean private RefreshTokenRepository refreshTokenRepository;

  @DisplayName("[M1-AUTH-03] rollbackWhenTransactionFailed")
  @Test
  void rollbackWhenTransactionFailed() {

    // given
    doThrow(DataIntegrityViolationException.class)
        .when(refreshTokenRepository)
        .save(any(RefreshToken.class));

    SignupRequest signupRequest =
        new SignupRequest(
            "guardian@example.com",
            "StrongPassword1!",
            ProfileType.INDIVIDUAL,
            Gender.MALE,
            IdentityVisibility.PUBLIC);

    assertThat(guardianRepository.count()).isEqualTo(0);
    assertThat(refreshTokenRepository.count()).isEqualTo(0);

    // when & then
    assertThatThrownBy(() -> authService.signup(signupRequest))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(guardianRepository.findByEmail("guardian@example.com")).isEmpty();
    assertThat(guardianRepository.count()).isEqualTo(0);
    assertThat(refreshTokenRepository.count()).isEqualTo(0);
  }
}

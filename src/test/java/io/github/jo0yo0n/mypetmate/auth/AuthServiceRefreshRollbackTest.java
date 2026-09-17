package io.github.jo0yo0n.mypetmate.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;

import io.github.jo0yo0n.mypetmate.auth.dto.AuthResponse;
import io.github.jo0yo0n.mypetmate.auth.dto.RefreshRequest;
import io.github.jo0yo0n.mypetmate.auth.dto.SignupRequest;
import io.github.jo0yo0n.mypetmate.guardian.domain.IdentityVisibility;
import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import io.github.jo0yo0n.mypetmate.guardian.persistence.RefreshToken;
import io.github.jo0yo0n.mypetmate.guardian.persistence.RefreshTokenRepository;
import io.github.jo0yo0n.mypetmate.support.PostgreSqlIntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
public class AuthServiceRefreshRollbackTest extends PostgreSqlIntegrationTestSupport {

  @Autowired private AuthService authService;
  @MockitoSpyBean private RefreshTokenRepository refreshTokenRepository;
  @MockitoSpyBean private RefreshTokenGenerator refreshTokenGenerator;

  private final SignupRequest signupRequest =
      new SignupRequest(
          "guardian@example.com",
          "StrongPassword!1",
          ProfileType.FAMILY,
          null,
          IdentityVisibility.PUBLIC);

  @DisplayName("[M1-AUTH-10] rollsBackRefreshTokenRotationWhenRepositorySaveThrows()")
  @Test
  void rollsBackRefreshTokenRotationWhenRepositorySaveThrows() {

    AuthResponse authResponse = authService.signup(signupRequest);
    long beforeRefreshTokenCount = refreshTokenRepository.count();
    willThrow(new DataIntegrityViolationException("save failed"))
        .given(refreshTokenRepository)
        .save(any());

    assertThatThrownBy(() -> authService.refresh(new RefreshRequest(authResponse.refreshToken())))
        .isInstanceOf(DataAccessException.class);

    assertRefreshTokenRotationWasRolledBack(authResponse, beforeRefreshTokenCount);
  }

  @DisplayName("[M1-AUTH-10] rollsBackRefreshTokenRotationWhenNewTokenHashViolatesUniqueConstraint")
  @Test
  void rollsBackRefreshTokenRotationWhenNewTokenHashViolatesUniqueConstraint() {

    AuthResponse authResponse = authService.signup(signupRequest);
    willReturn(authResponse.refreshToken()).given(refreshTokenGenerator).generate();
    long beforeRefreshTokenCount = refreshTokenRepository.count();

    assertThatThrownBy(() -> authService.refresh(new RefreshRequest(authResponse.refreshToken())))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertRefreshTokenRotationWasRolledBack(authResponse, beforeRefreshTokenCount);
  }

  private void assertRefreshTokenRotationWasRolledBack(
      AuthResponse authResponse, long beforeRefreshTokenCount) {
    RefreshToken afterRefreshToken =
        refreshTokenRepository
            .findByTokenHash(refreshTokenGenerator.hash(authResponse.refreshToken()))
            .orElseThrow();

    assertThat(afterRefreshToken.getRevokedAt()).isNull();
    assertThat(refreshTokenRepository.count()).isEqualTo(beforeRefreshTokenCount);
  }
}

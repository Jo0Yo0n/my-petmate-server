package io.github.jo0yo0n.mypetmate.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.jo0yo0n.mypetmate.auth.dto.LoginRequest;
import io.github.jo0yo0n.mypetmate.auth.exception.InvalidCredentialsException;
import io.github.jo0yo0n.mypetmate.config.TokenProperties;
import io.github.jo0yo0n.mypetmate.guardian.domain.GuardianStatus;
import io.github.jo0yo0n.mypetmate.guardian.domain.IdentityVisibility;
import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import io.github.jo0yo0n.mypetmate.guardian.persistence.Guardian;
import io.github.jo0yo0n.mypetmate.guardian.persistence.GuardianRepository;
import io.github.jo0yo0n.mypetmate.guardian.persistence.RefreshTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTest {

  @Mock Clock clock;
  @Mock AccessTokenIssuer accessTokenIssuer;
  @Mock GuardianRepository guardianRepository;
  @Mock RefreshTokenRepository refreshTokenRepository;
  @Mock RefreshTokenGenerator refreshTokenGenerator;
  @Mock PasswordEncoder passwordEncoder;

  private static final long ACCESS_TOKEN_TTL = 900;
  private static final long REFRESH_TOKEN_TTL = 2592000;
  private static final String TOKEN_TYPE = "Bearer";
  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService =
        new AuthService(
            clock,
            accessTokenIssuer,
            new TokenProperties(
                Duration.ofSeconds(ACCESS_TOKEN_TTL),
                Duration.ofSeconds(REFRESH_TOKEN_TTL),
                TOKEN_TYPE),
            guardianRepository,
            refreshTokenRepository,
            refreshTokenGenerator,
            passwordEncoder);
  }

  private enum InvalidLoginCase {
    UNKNOWN_EMAIL,
    INCORRECT_PASSWORD
  }

  static Stream<InvalidLoginCase> invalidLoginCases() {
    return Stream.of(InvalidLoginCase.UNKNOWN_EMAIL, InvalidLoginCase.INCORRECT_PASSWORD);
  }

  @DisplayName("[M1-AUTH-05] rejectsInvalidCredentialsWithoutIssuingTokens")
  @ParameterizedTest
  @MethodSource("invalidLoginCases")
  void rejectsInvalidCredentialsWithoutIssuingTokens(InvalidLoginCase invalidLoginCase) {

    LoginRequest loginRequest =
        switch (invalidLoginCase) {
          case UNKNOWN_EMAIL -> {
            String email = "missing@email.com";
            String rawPassword = "StrongPassword1!";
            given(guardianRepository.findByEmail(email)).willReturn(Optional.empty());

            yield new LoginRequest(email, rawPassword);
          }

          case INCORRECT_PASSWORD -> {
            Guardian guardian = newGuardian();
            String email = "guardian@example.com";
            String rawPassword = "StrongPassword1!";

            given(guardianRepository.findByEmail(email)).willReturn(Optional.of(guardian));
            given(passwordEncoder.matches(rawPassword, "hashed-password")).willReturn(false);

            yield new LoginRequest(email, rawPassword);
          }
        };

    assertThatThrownBy(() -> authService.login(loginRequest))
        .isInstanceOf(InvalidCredentialsException.class);
    verifyNoInteractions(accessTokenIssuer);
    verifyNoInteractions(refreshTokenGenerator);
    verifyNoInteractions(refreshTokenRepository);

    if (invalidLoginCase == InvalidLoginCase.INCORRECT_PASSWORD) {
      verify(passwordEncoder).matches("StrongPassword1!", "hashed-password");
    }
  }

  private Guardian newGuardian() {
    return new Guardian(
        UUID.randomUUID(),
        "guardian@example.com",
        "hashed-password",
        ProfileType.FAMILY,
        null,
        IdentityVisibility.PUBLIC,
        GuardianStatus.ACTIVE,
        NOW,
        NOW);
  }
}

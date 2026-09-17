package io.github.jo0yo0n.mypetmate.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;

import io.github.jo0yo0n.mypetmate.auth.dto.AuthResponse;
import io.github.jo0yo0n.mypetmate.auth.dto.LoginRequest;
import io.github.jo0yo0n.mypetmate.auth.dto.RefreshRequest;
import io.github.jo0yo0n.mypetmate.auth.dto.SignupRequest;
import io.github.jo0yo0n.mypetmate.auth.dto.TokenResponse;
import io.github.jo0yo0n.mypetmate.auth.exception.EmailAlreadyExistsException;
import io.github.jo0yo0n.mypetmate.auth.exception.InvalidCredentialsException;
import io.github.jo0yo0n.mypetmate.auth.exception.InvalidRefreshTokenException;
import io.github.jo0yo0n.mypetmate.config.TokenProperties;
import io.github.jo0yo0n.mypetmate.guardian.domain.Gender;
import io.github.jo0yo0n.mypetmate.guardian.domain.GuardianStatus;
import io.github.jo0yo0n.mypetmate.guardian.domain.IdentityVisibility;
import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import io.github.jo0yo0n.mypetmate.guardian.dto.GuardianResponse;
import io.github.jo0yo0n.mypetmate.guardian.persistence.Guardian;
import io.github.jo0yo0n.mypetmate.guardian.persistence.GuardianRepository;
import io.github.jo0yo0n.mypetmate.guardian.persistence.RefreshToken;
import io.github.jo0yo0n.mypetmate.guardian.persistence.RefreshTokenRepository;
import io.github.jo0yo0n.mypetmate.support.PostgreSqlIntegrationTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
public class AuthServiceIntegrationTest extends PostgreSqlIntegrationTestSupport {

  @Autowired private AuthService authService;
  @Autowired private GuardianRepository guardianRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private RefreshTokenGenerator refreshTokenGenerator;
  @Autowired private JwtDecoder jwtDecoder;
  @Autowired private TokenProperties tokenProperties;
  @MockitoSpyBean private AccessTokenIssuer accessTokenIssuer;
  @PersistenceContext private EntityManager entityManager;

  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
  private static final String CANONICAL_EMAIL = "guardian@example.com";

  private enum LoginCase {
    WITHDRAWN,
    TEMPORARILY_RESTRICT
  }

  private enum InvalidRefreshToken {
    EXPIRED,
    EXPIRES_AT_NOW,
    REVOKED,
    ROTATED,
    UNKNOWN
  }

  static Stream<LoginCase> loginCases() {
    return Stream.of(LoginCase.WITHDRAWN, LoginCase.TEMPORARILY_RESTRICT);
  }

  static Stream<InvalidRefreshToken> invalidRefreshTokenCases() {
    return Stream.of(
        InvalidRefreshToken.EXPIRED,
        InvalidRefreshToken.EXPIRES_AT_NOW,
        InvalidRefreshToken.REVOKED,
        InvalidRefreshToken.ROTATED,
        InvalidRefreshToken.UNKNOWN);
  }

  static Stream<InvalidRefreshToken> invalidRefreshTokenCasesForLogout() {
    return Stream.of(
        InvalidRefreshToken.EXPIRED,
        InvalidRefreshToken.EXPIRES_AT_NOW,
        InvalidRefreshToken.REVOKED,
        InvalidRefreshToken.UNKNOWN);
  }

  @DisplayName("[M1-AUTH-01] successSignUp")
  @Test
  @Transactional
  void successSignUp() {
    // Given
    String rawPassword = "StrongPass123!";
    SignupRequest signupRequest =
        new SignupRequest(
            "Guardian@example.com  ",
            rawPassword,
            ProfileType.INDIVIDUAL,
            Gender.MALE,
            IdentityVisibility.PUBLIC);

    // When
    AuthResponse authResponse = authService.signup(signupRequest);

    // Then: Guardian persistence
    Guardian guardian = guardianRepository.findByEmail(CANONICAL_EMAIL).orElseThrow();

    assertThat(guardian.getEmail()).isEqualTo(CANONICAL_EMAIL);
    assertThat(guardian.getProfileType()).isEqualTo(ProfileType.INDIVIDUAL);
    assertThat(guardian.getGender()).isEqualTo(Gender.MALE);
    assertThat(guardian.getIdentityVisibility()).isEqualTo(IdentityVisibility.PUBLIC);
    assertThat(guardian.getStatus()).isEqualTo(GuardianStatus.ACTIVE);
    assertThat(guardian.getPasswordHash()).isNotEqualTo(rawPassword);
    assertThat(guardian.getPasswordHash()).startsWith("$2");
    assertThat(passwordEncoder.matches(rawPassword, guardian.getPasswordHash())).isTrue();

    // Then: Guardian response
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByTokenHash(refreshTokenGenerator.hash(authResponse.refreshToken()))
            .orElseThrow();

    assertAuthResponseMatchesContract(authResponse, guardian, refreshToken);
  }

  @DisplayName("[M1-AUTH-02] rejectsSignupWithSameEmail")
  @Test
  @Transactional
  void rejectsSignupWithSameEmail() {
    String existEmail = "guardian@example.com";
    String rawPassword = "StrongPass123!";
    SignupRequest signupRequest =
        new SignupRequest(
            "  Guardian@ExAmPlE.com ",
            rawPassword,
            ProfileType.INDIVIDUAL,
            Gender.MALE,
            IdentityVisibility.PUBLIC);
    authService.signup(signupRequest);
    long guardianCountBefore = guardianRepository.count();
    long refreshTokenCountBefore = refreshTokenRepository.count();

    SignupRequest duplicatedSignupRequest =
        new SignupRequest(
            existEmail,
            rawPassword + "a",
            ProfileType.INDIVIDUAL,
            Gender.FEMALE,
            IdentityVisibility.PUBLIC);

    assertThatThrownBy(() -> authService.signup(duplicatedSignupRequest))
        .isInstanceOf(EmailAlreadyExistsException.class);
    assertThat(guardianRepository.count()).isEqualTo(guardianCountBefore);
    assertThat(refreshTokenRepository.count()).isEqualTo(refreshTokenCountBefore);
  }

  @DisplayName("[M1-AUTH-04] successLogin")
  @Test
  @Transactional
  void successLogin() {

    // Given
    String rawPassword = "StrongPass123!";

    AuthResponse signupResponse =
        authService.signup(
            new SignupRequest(
                "guardian@example.com",
                rawPassword,
                ProfileType.INDIVIDUAL,
                Gender.MALE,
                IdentityVisibility.PUBLIC));
    long guardianCountBefore = guardianRepository.count();
    long refreshTokenCountBefore = refreshTokenRepository.count();

    Guardian guardian = guardianRepository.findByEmail(CANONICAL_EMAIL).orElseThrow();

    // When
    AuthResponse authResponse =
        authService.login(new LoginRequest("  Guardian@ExAmPlE.com ", rawPassword));

    // Then
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByTokenHash(refreshTokenGenerator.hash(authResponse.refreshToken()))
            .orElseThrow();

    assertAuthResponseMatchesContract(authResponse, guardian, refreshToken);

    // Then: new token pair is issued
    assertThat(authResponse.accessToken()).isNotEqualTo(signupResponse.accessToken());
    assertThat(authResponse.refreshToken()).isNotEqualTo(signupResponse.refreshToken());
    assertThat(guardianRepository.count()).isEqualTo(guardianCountBefore);
    assertThat(refreshTokenRepository.count()).isEqualTo(refreshTokenCountBefore + 1);
  }

  @DisplayName("[M1-AUTH-06] loginRejectsWithdrawnGuardianAndAllowsTemporarilyRestrictedGuardian")
  @ParameterizedTest
  @MethodSource("loginCases")
  @Transactional
  void loginRejectsWithdrawnGuardianAndAllowsTemporarilyRestrictedGuardian(LoginCase loginCase) {

    String email = "guardian@example.com";
    String rawPassword = "StrongPassword!1";
    String hashedPassword = passwordEncoder.encode(rawPassword);
    LoginRequest loginRequest = new LoginRequest(email, rawPassword);

    switch (loginCase) {
      case WITHDRAWN -> {
        guardianRepository.saveAndFlush(newGuardian(GuardianStatus.WITHDRAWN, hashedPassword));

        assertThatThrownBy(() -> authService.login(loginRequest))
            .isInstanceOf(InvalidCredentialsException.class);

        then(accessTokenIssuer).should(never()).issue(any(), any());
        assertThat(refreshTokenRepository.count()).isEqualTo(0);
      }

      case TEMPORARILY_RESTRICT -> {
        guardianRepository.saveAndFlush(
            newGuardian(GuardianStatus.TEMPORARILY_RESTRICTED, hashedPassword));
        Guardian guardian = guardianRepository.findByEmail(email).orElseThrow();

        AuthResponse authResponse = authService.login(loginRequest);
        assertThat(authResponse).isNotNull();
        assertThat(guardian.getId()).isEqualTo(authResponse.guardian().id());
        assertThat(authResponse.guardian().status())
            .isEqualTo(GuardianStatus.TEMPORARILY_RESTRICTED);
        assertThat(jwtDecoder.decode(authResponse.accessToken()).getSubject())
            .isEqualTo(guardian.getId().toString());

        RefreshToken refreshToken =
            refreshTokenRepository
                .findByTokenHash(refreshTokenGenerator.hash(authResponse.refreshToken()))
                .orElseThrow();
        assertThat(refreshToken.getGuardian().getId()).isEqualTo(guardian.getId());

        then(accessTokenIssuer).should().issue(any(), any());
        assertThat(refreshTokenRepository.count()).isEqualTo(1);
      }
    }
  }

  @DisplayName("[M1-AUTH-07] successRefresh")
  @Test
  @Transactional
  void successRefresh() {

    AuthResponse authResponse =
        authService.signup(
            new SignupRequest(
                "guardian@example.com",
                "StrongPassword1!",
                ProfileType.FAMILY,
                null,
                IdentityVisibility.PUBLIC));

    Guardian guardian = guardianRepository.findByEmail("guardian@example.com").orElseThrow();

    // when
    TokenResponse tokenResponse =
        authService.refresh(new RefreshRequest(authResponse.refreshToken()));

    entityManager.flush();
    entityManager.clear();

    RefreshToken beforeRefresh =
        refreshTokenRepository
            .findByTokenHash(refreshTokenGenerator.hash(authResponse.refreshToken()))
            .orElseThrow();

    RefreshToken afterRefresh =
        refreshTokenRepository
            .findByTokenHash(refreshTokenGenerator.hash(tokenResponse.refreshToken()))
            .orElseThrow();

    // then
    assertThat(tokenResponse.refreshToken()).isNotEqualTo(authResponse.refreshToken());
    assertThat(tokenResponse.refreshToken()).isNotEqualTo(afterRefresh.getTokenHash());
    assertThat(refreshTokenGenerator.hash(tokenResponse.refreshToken()))
        .isEqualTo(afterRefresh.getTokenHash());
    assertThat(beforeRefresh.getRevokedAt()).isEqualTo(NOW);
    assertThat(afterRefresh.getGuardian().getId()).isEqualTo(guardian.getId());
    assertThat(afterRefresh.getExpiresAt()).isEqualTo(NOW.plus(tokenProperties.refreshTokenTtl()));
    assertThat(refreshTokenRepository.count()).isEqualTo(2);

    Jwt decodedLegacyAccessToken = jwtDecoder.decode(authResponse.accessToken());
    Jwt decodedNewAccessToken = jwtDecoder.decode(tokenResponse.accessToken());
    assertThat(decodedNewAccessToken.getId()).isNotEqualTo(decodedLegacyAccessToken.getId());
    assertThat(decodedNewAccessToken.getSubject()).isEqualTo(guardian.getId().toString());
    assertThat(tokenResponse.expiresIn())
        .isEqualTo(Math.toIntExact(tokenProperties.accessTokenTtl().toSeconds()));
    assertThat(tokenResponse.refreshExpiresIn())
        .isEqualTo(Math.toIntExact(tokenProperties.refreshTokenTtl().toSeconds()));
    assertThat(tokenResponse.tokenType()).isEqualTo(tokenProperties.tokenType());
  }

  @DisplayName("[M1-AUTH-08] rejectsInvalidRefreshToken")
  @ParameterizedTest
  @MethodSource("invalidRefreshTokenCases")
  @Transactional
  void rejectsInvalidRefreshToken(InvalidRefreshToken invalidRefreshToken) {

    Guardian guardian =
        guardianRepository.saveAndFlush(newGuardian(GuardianStatus.ACTIVE, "hashed-password"));

    record RefreshFixture(String rawRefreshToken, long countBefore) {}

    RefreshFixture refreshFixture =
        switch (invalidRefreshToken) {
          case EXPIRED -> {
            String generatedRefreshToken = refreshTokenGenerator.generate();
            RefreshToken refreshToken =
                new RefreshToken(
                    UUID.randomUUID(),
                    guardian,
                    refreshTokenGenerator.hash(generatedRefreshToken),
                    NOW.minus(Duration.ofSeconds(1)),
                    null,
                    NOW.minus(Duration.ofDays(1)));

            refreshTokenRepository.save(refreshToken);

            yield new RefreshFixture(generatedRefreshToken, refreshTokenRepository.count());
          }

          case EXPIRES_AT_NOW -> {
            String generatedRefreshToken = refreshTokenGenerator.generate();
            RefreshToken refreshToken =
                new RefreshToken(
                    UUID.randomUUID(),
                    guardian,
                    refreshTokenGenerator.hash(generatedRefreshToken),
                    NOW,
                    null,
                    NOW.minus(Duration.ofDays(1)));

            refreshTokenRepository.save(refreshToken);

            yield new RefreshFixture(generatedRefreshToken, refreshTokenRepository.count());
          }

          case REVOKED -> {
            String generatedRefreshToken = refreshTokenGenerator.generate();
            RefreshToken refreshToken =
                new RefreshToken(
                    UUID.randomUUID(),
                    guardian,
                    refreshTokenGenerator.hash(generatedRefreshToken),
                    NOW.plus(tokenProperties.refreshTokenTtl()),
                    NOW,
                    NOW);

            refreshTokenRepository.save(refreshToken);

            yield new RefreshFixture(generatedRefreshToken, refreshTokenRepository.count());
          }

          case ROTATED -> {
            String generatedRefreshToken = refreshTokenGenerator.generate();
            RefreshToken refreshToken =
                new RefreshToken(
                    UUID.randomUUID(),
                    guardian,
                    refreshTokenGenerator.hash(generatedRefreshToken),
                    NOW.plus(tokenProperties.refreshTokenTtl()),
                    null,
                    NOW);

            refreshTokenRepository.save(refreshToken);

            authService.refresh(new RefreshRequest(generatedRefreshToken));

            yield new RefreshFixture(generatedRefreshToken, refreshTokenRepository.count());
          }

          case UNKNOWN -> {
            String generatedRefreshToken = refreshTokenGenerator.generate();

            RefreshToken refreshToken =
                new RefreshToken(
                    UUID.randomUUID(),
                    guardian,
                    refreshTokenGenerator.hash(generatedRefreshToken),
                    NOW.plus(tokenProperties.refreshTokenTtl()),
                    null,
                    NOW);

            refreshTokenRepository.save(refreshToken);

            String unknownRefreshToken = refreshTokenGenerator.generate();

            yield new RefreshFixture(unknownRefreshToken, refreshTokenRepository.count());
          }
        };

    clearInvocations(accessTokenIssuer);
    assertThatThrownBy(
            () -> authService.refresh(new RefreshRequest(refreshFixture.rawRefreshToken)))
        .isInstanceOf(InvalidRefreshTokenException.class);
    assertThat(refreshTokenRepository.count()).isEqualTo(refreshFixture.countBefore);
    then(accessTokenIssuer).should(never()).issue(any(), any());
  }

  @DisplayName("[M1-AUTH-11] successLogoutWithValidToken")
  @Test
  @Transactional
  void successLogoutWithValidToken() {

    Guardian guardian =
        guardianRepository.saveAndFlush(newGuardian(GuardianStatus.ACTIVE, "hashed-password"));

    String generatedRefreshToken = refreshTokenGenerator.generate();
    refreshTokenRepository.save(
        new RefreshToken(
            UUID.randomUUID(),
            guardian,
            refreshTokenGenerator.hash(generatedRefreshToken),
            NOW.plus(tokenProperties.refreshTokenTtl()),
            null,
            NOW));
    long refreshTokenCountBefore = refreshTokenRepository.count();

    authService.logout(new RefreshRequest(generatedRefreshToken));

    entityManager.flush();
    entityManager.clear();

    RefreshToken refreshToken =
        refreshTokenRepository
            .findByTokenHash(refreshTokenGenerator.hash(generatedRefreshToken))
            .orElseThrow();
    assertThat(refreshToken.getRevokedAt()).isNotNull();
    assertThat(refreshTokenRepository.count()).isEqualTo(refreshTokenCountBefore);
  }

  @DisplayName("[M1-AUTH-11] successLogoutWithInvalidTokens")
  @ParameterizedTest
  @MethodSource("invalidRefreshTokenCasesForLogout")
  @Transactional
  void successLogoutWithInvalidTokens(InvalidRefreshToken invalidRefreshToken) {

    Guardian guardian =
        guardianRepository.saveAndFlush(newGuardian(GuardianStatus.ACTIVE, "hashed-password"));
    String generatedRefreshToken = refreshTokenGenerator.generate();

    switch (invalidRefreshToken) {
      case EXPIRED -> {
        RefreshToken legacyRefreshToken =
            new RefreshToken(
                UUID.randomUUID(),
                guardian,
                refreshTokenGenerator.hash(generatedRefreshToken),
                NOW.minus(Duration.ofSeconds(1)),
                null,
                NOW.minus(Duration.ofDays(1)));

        refreshTokenRepository.save(legacyRefreshToken);

        long refreshTokenCountBefore = refreshTokenRepository.count();

        authService.logout(new RefreshRequest(generatedRefreshToken));

        entityManager.flush();
        entityManager.clear();

        assertThat(
                refreshTokenRepository.findByTokenHash(
                    refreshTokenGenerator.hash(generatedRefreshToken)))
            .isEmpty();
        assertThat(refreshTokenRepository.count()).isEqualTo(refreshTokenCountBefore - 1);
      }

      case EXPIRES_AT_NOW -> {
        RefreshToken legacyRefreshToken =
            new RefreshToken(
                UUID.randomUUID(),
                guardian,
                refreshTokenGenerator.hash(generatedRefreshToken),
                NOW,
                null,
                NOW.minus(Duration.ofDays(1)));

        refreshTokenRepository.save(legacyRefreshToken);

        long refreshTokenCountBefore = refreshTokenRepository.count();

        authService.logout(new RefreshRequest(generatedRefreshToken));

        entityManager.flush();
        entityManager.clear();

        assertThat(
                refreshTokenRepository.findByTokenHash(
                    refreshTokenGenerator.hash(generatedRefreshToken)))
            .isEmpty();
        assertThat(refreshTokenRepository.count()).isEqualTo(refreshTokenCountBefore - 1);
      }

      case REVOKED -> {
        RefreshToken legacyRefreshToken =
            new RefreshToken(
                UUID.randomUUID(),
                guardian,
                refreshTokenGenerator.hash(generatedRefreshToken),
                NOW.plus(tokenProperties.refreshTokenTtl()),
                NOW.minus(Duration.ofSeconds(1)),
                NOW.minus(Duration.ofSeconds(2)));

        refreshTokenRepository.save(legacyRefreshToken);

        long refreshTokenCountBefore = refreshTokenRepository.count();

        authService.logout(new RefreshRequest(generatedRefreshToken));

        entityManager.flush();
        entityManager.clear();

        RefreshToken refreshToken =
            refreshTokenRepository
                .findByTokenHash(refreshTokenGenerator.hash(generatedRefreshToken))
                .orElseThrow();

        assertThat(refreshToken.getRevokedAt()).isEqualTo(NOW.minus(Duration.ofSeconds(1)));
        assertThat(refreshTokenRepository.count()).isEqualTo(refreshTokenCountBefore);
      }

      case UNKNOWN -> {
        RefreshToken legacyRefreshToken =
            new RefreshToken(
                UUID.randomUUID(),
                guardian,
                refreshTokenGenerator.hash(generatedRefreshToken),
                NOW.plus(tokenProperties.refreshTokenTtl()),
                null,
                NOW);

        refreshTokenRepository.save(legacyRefreshToken);

        long refreshTokenCountBefore = refreshTokenRepository.count();

        authService.logout(new RefreshRequest(refreshTokenGenerator.generate()));

        entityManager.flush();
        entityManager.clear();

        RefreshToken refreshToken =
            refreshTokenRepository
                .findByTokenHash(refreshTokenGenerator.hash(generatedRefreshToken))
                .orElseThrow();

        assertThat(refreshToken.getRevokedAt()).isNull();
        assertThat(refreshTokenRepository.count()).isEqualTo(refreshTokenCountBefore);
      }

      case null, default -> {
        throw new IllegalArgumentException("Unsupported Enum value");
      }
    }
  }

  private Guardian newGuardian(GuardianStatus status, String hashedPassword) {
    return new Guardian(
        UUID.randomUUID(),
        "guardian@example.com",
        hashedPassword,
        ProfileType.FAMILY,
        null,
        IdentityVisibility.PUBLIC,
        status,
        NOW,
        NOW);
  }

  private void assertAuthResponseMatchesContract(
      AuthResponse response, Guardian guardian, RefreshToken refreshToken) {

    GuardianResponse guardianResponse = response.guardian();

    // Then: Guardian response
    assertThat(guardianResponse.id()).isEqualTo(guardian.getId());
    assertThat(response.guardian().email()).isEqualTo(CANONICAL_EMAIL);
    assertThat(response.guardian().profileType()).isEqualTo(ProfileType.INDIVIDUAL);
    assertThat(response.guardian().gender()).isEqualTo(Gender.MALE);
    assertThat(response.guardian().identityVisibility()).isEqualTo(IdentityVisibility.PUBLIC);
    assertThat(response.guardian().status()).isEqualTo(GuardianStatus.ACTIVE);

    // Then: Refresh token persistence
    assertThat(refreshTokenGenerator.hash(response.refreshToken()))
        .isEqualTo(refreshToken.getTokenHash());
    assertThat(refreshToken.getGuardian().getId()).isEqualTo(guardian.getId());
    assertThat(refreshToken.getExpiresAt()).isEqualTo(NOW.plus(tokenProperties.refreshTokenTtl()));
    assertThat(refreshToken.getTokenHash()).isNotEqualTo(response.refreshToken());

    // Then: Access token and response metadata
    Jwt jwt = jwtDecoder.decode(response.accessToken());
    assertThat(jwt.getSubject()).isEqualTo(guardian.getId().toString());
    assertThat(response.tokenType()).isEqualTo(tokenProperties.tokenType());
    assertThat(response.expiresIn())
        .isEqualTo(Math.toIntExact(tokenProperties.accessTokenTtl().toSeconds()));
    assertThat(response.refreshExpiresIn())
        .isEqualTo(Math.toIntExact(tokenProperties.refreshTokenTtl().toSeconds()));
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class FixedClockConfiguration {

    @Bean
    @Primary
    Clock fixedClock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }
}

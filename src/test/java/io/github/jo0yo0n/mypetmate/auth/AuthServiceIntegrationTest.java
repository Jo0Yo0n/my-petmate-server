package io.github.jo0yo0n.mypetmate.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.jo0yo0n.mypetmate.auth.exception.EmailAlreadyExistsException;
import io.github.jo0yo0n.mypetmate.config.TokenProperties;
import io.github.jo0yo0n.mypetmate.guardian.domain.Gender;
import io.github.jo0yo0n.mypetmate.guardian.domain.GuardianStatus;
import io.github.jo0yo0n.mypetmate.guardian.domain.IdentityVisibility;
import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import io.github.jo0yo0n.mypetmate.guardian.dto.AuthResponse;
import io.github.jo0yo0n.mypetmate.guardian.dto.SignupRequest;
import io.github.jo0yo0n.mypetmate.guardian.persistence.Guardian;
import io.github.jo0yo0n.mypetmate.guardian.persistence.GuardianRepository;
import io.github.jo0yo0n.mypetmate.guardian.persistence.RefreshToken;
import io.github.jo0yo0n.mypetmate.guardian.persistence.RefreshTokenRepository;
import io.github.jo0yo0n.mypetmate.support.PostgreSqlIntegrationTestSupport;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
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

  static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

  @DisplayName("[M1-AUTH-01] successSignUp")
  @Test
  @Transactional
  void successSignUp() {
    // Given
    String expectedEmail = "guardian@example.com";
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
    Guardian guardian = guardianRepository.findByEmail(expectedEmail).orElseThrow();
    assertThat(guardian.getEmail()).isEqualTo(expectedEmail);
    assertThat(guardian.getProfileType()).isEqualTo(ProfileType.INDIVIDUAL);
    assertThat(guardian.getGender()).isEqualTo(Gender.MALE);
    assertThat(guardian.getIdentityVisibility()).isEqualTo(IdentityVisibility.PUBLIC);
    assertThat(guardian.getStatus()).isEqualTo(GuardianStatus.ACTIVE);
    assertThat(guardian.getPasswordHash()).isNotEqualTo(rawPassword);
    assertThat(guardian.getPasswordHash()).startsWith("$2");
    assertThat(passwordEncoder.matches(rawPassword, guardian.getPasswordHash())).isTrue();

    // Then: Guardian response
    assertThat(authResponse.guardian().id()).isEqualTo(guardian.getId());
    assertThat(authResponse.guardian().email()).isEqualTo(expectedEmail);
    assertThat(authResponse.guardian().profileType()).isEqualTo(ProfileType.INDIVIDUAL);
    assertThat(authResponse.guardian().gender()).isEqualTo(Gender.MALE);
    assertThat(authResponse.guardian().identityVisibility()).isEqualTo(IdentityVisibility.PUBLIC);
    assertThat(authResponse.guardian().status()).isEqualTo(GuardianStatus.ACTIVE);

    // Then: Refresh token persistence
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByTokenHash(refreshTokenGenerator.hash(authResponse.refreshToken()))
            .orElseThrow();

    assertThat(refreshTokenGenerator.hash(authResponse.refreshToken()))
        .isEqualTo(refreshToken.getTokenHash());
    assertThat(refreshToken.getGuardian().getId()).isEqualTo(guardian.getId());
    assertThat(refreshToken.getExpiresAt()).isEqualTo(NOW.plus(tokenProperties.refreshTokenTtl()));
    assertThat(refreshToken.getTokenHash()).isNotEqualTo(authResponse.refreshToken());

    // Then: Access token and response metadata
    Jwt jwt = jwtDecoder.decode(authResponse.accessToken());
    assertThat(jwt.getSubject()).isEqualTo(guardian.getId().toString());
    assertThat(authResponse.tokenType()).isEqualTo(tokenProperties.tokenType());
    assertThat(authResponse.expiresIn())
        .isEqualTo(Math.toIntExact(tokenProperties.accessTokenTtl().toSeconds()));
    assertThat(authResponse.refreshExpiresIn())
        .isEqualTo(Math.toIntExact(tokenProperties.refreshTokenTtl().toSeconds()));
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

  @TestConfiguration(proxyBeanMethods = false)
  static class FixedClockConfiguration {

    @Bean
    @Primary
    Clock fixedClock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }
}

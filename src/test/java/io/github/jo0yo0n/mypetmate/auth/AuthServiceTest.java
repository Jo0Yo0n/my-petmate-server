package io.github.jo0yo0n.mypetmate.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.github.jo0yo0n.mypetmate.config.JwtConfig;
import io.github.jo0yo0n.mypetmate.config.JwtProperties;
import io.github.jo0yo0n.mypetmate.config.TokenProperties;
import io.github.jo0yo0n.mypetmate.guardian.domain.Gender;
import io.github.jo0yo0n.mypetmate.guardian.domain.GuardianStatus;
import io.github.jo0yo0n.mypetmate.guardian.domain.IdentityVisibility;
import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import io.github.jo0yo0n.mypetmate.guardian.persistence.Guardian;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

public class AuthServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-28T00:00:00Z");
  private static final String SECRET = "s".repeat(64);
  private static final String ISSUER = "my-petmate-server";
  private static final String AUDIENCE = "my-petmate-api";
  private static final String ACCESS_TOKEN_TTL = "900s";
  private static final String REFRESH_TOKEN_TTL = "2592000s";
  private static final String TOKEN_TYPE = "Bearer";

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(JwtTestConfiguration.class)
          .withPropertyValues(
              "app.jwt.secret=" + SECRET,
              "app.jwt.issuer=" + ISSUER,
              "app.jwt.audience=" + AUDIENCE,
              "app.token.access-token-ttl=" + ACCESS_TOKEN_TTL,
              "app.token.refresh-token-ttl=" + REFRESH_TOKEN_TTL,
              "app.token.token-type=" + TOKEN_TYPE);

  @Test
  void issuesAnAccessTokenWithContractClaims() {
    contextRunner.run(
        context -> {
          Guardian guardian =
              new Guardian(
                  UUID.randomUUID(),
                  "guardian@test.com",
                  "hashed-password",
                  ProfileType.INDIVIDUAL,
                  Gender.MALE,
                  IdentityVisibility.PUBLIC,
                  GuardianStatus.ACTIVE,
                  NOW,
                  null);

          AuthService authService =
              new AuthService(
                  context.getBean(Clock.class),
                  context.getBean(JwtEncoder.class),
                  context.getBean(JwtProperties.class),
                  context.getBean(TokenProperties.class));

          String accessToken = authService.issueAccessToken(guardian);
          JwtDecoder jwtDecoder = context.getBean(JwtDecoder.class);
          Jwt decoded = jwtDecoder.decode(accessToken);

          assertThat(decoded.getHeaders()).containsEntry("alg", MacAlgorithm.HS256.getName());
          assertThat(decoded.getId()).isNotBlank();
          assertThatCode(() -> UUID.fromString(decoded.getId())).doesNotThrowAnyException();
          assertThat(decoded.getSubject()).isEqualTo(guardian.getId().toString());
          assertThat(decoded.getClaimAsString("iss")).isEqualTo(ISSUER);
          assertThat(decoded.getAudience()).containsExactly(AUDIENCE);
          assertThat(decoded.getIssuedAt()).isEqualTo(NOW);
          assertThat(decoded.getExpiresAt()).isEqualTo(NOW.plusSeconds(900));
          assertThat(decoded.getClaims())
              .containsOnlyKeys("sub", "iss", "aud", "iat", "exp", "jti");
        });
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties({JwtProperties.class, TokenProperties.class})
  @Import(JwtConfig.class)
  static class JwtTestConfiguration {

    @Bean
    Clock clock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }
}

package io.github.jo0yo0n.mypetmate.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtConfigTest {

  private static final String SECRET = "s".repeat(64);
  private static final String ISSUER = "my-petmate-server";
  private static final String AUDIENCE = "my-petmate-api";
  private static final Instant NOW = Instant.parse("2026-08-28T00:00:00Z");

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(JwtTestConfiguration.class)
          .withPropertyValues(
              "app.jwt.secret=" + SECRET,
              "app.jwt.issuer=" + ISSUER,
              "app.jwt.audience=" + AUDIENCE);

  @Test
  void encodesAndDecodesAnHs256JwtWithTheConfiguredIssuerAndAudience() {
    contextRunner.run(
        context -> {
          JwtEncoder encoder = context.getBean(JwtEncoder.class);
          JwtDecoder decoder = context.getBean(JwtDecoder.class);

          Jwt decoded = decoder.decode(encode(encoder, validClaims().build()));

          assertThat(decoded.getHeaders()).containsEntry("alg", MacAlgorithm.HS256.getName());
          assertThat(decoded.getSubject()).isEqualTo("guardian-id");
          assertThat(decoded.getClaimAsString("iss")).isEqualTo(ISSUER);
          assertThat(decoded.getAudience()).containsExactly(AUDIENCE);
          assertThat(decoded.getIssuedAt()).isEqualTo(NOW);
          assertThat(decoded.getExpiresAt()).isEqualTo(NOW.plusSeconds(900));
        });
  }

  @Test
  void rejectsJwtWithAnUnexpectedIssuer() {
    contextRunner.run(
        context -> {
          JwtEncoder encoder = context.getBean(JwtEncoder.class);
          JwtDecoder decoder = context.getBean(JwtDecoder.class);

          String token = encode(encoder, validClaims().issuer("another-issuer").build());

          assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
        });
  }

  @Test
  void rejectsJwtWithAnUnexpectedAudience() {
    contextRunner.run(
        context -> {
          JwtEncoder encoder = context.getBean(JwtEncoder.class);
          JwtDecoder decoder = context.getBean(JwtDecoder.class);

          String token = encode(encoder, validClaims().audience(List.of("another-api")).build());

          assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
        });
  }

  @Test
  void rejectsExpiredJwtWithoutClockSkew() {
    contextRunner.run(
        context -> {
          JwtEncoder encoder = context.getBean(JwtEncoder.class);
          JwtDecoder decoder = context.getBean(JwtDecoder.class);
          JwtClaimsSet expiredClaims =
              validClaims().expiresAt(NOW.minusSeconds(1)).issuedAt(NOW.minusSeconds(901)).build();

          String token = encode(encoder, expiredClaims);

          assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
        });
  }

  @Test
  void rejectsJwtSignedWithAnAlgorithmOtherThanHs256() {
    contextRunner.run(
        context -> {
          JwtDecoder decoder = context.getBean(JwtDecoder.class);
          String token = hs384Token(validClaims().build());

          assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
        });
  }

  @Test
  void rejectsJwtWithForgerySignature() {
    contextRunner.run(
        context -> {
          JwtEncoder encoder = context.getBean(JwtEncoder.class);
          JwtDecoder decoder = context.getBean(JwtDecoder.class);

          String token = encode(encoder, validClaims().build());

          int signatureStartIndex = token.lastIndexOf('.') + 1;
          char original = token.charAt(signatureStartIndex);
          char replacement = original == 'A' ? 'B' : 'A';

          String forgedToken =
              token.substring(0, signatureStartIndex)
                  + replacement
                  + token.substring(signatureStartIndex + 1);

          assertThatThrownBy(() -> decoder.decode(forgedToken)).isInstanceOf(JwtException.class);
        });
  }

  private String encode(JwtEncoder encoder, JwtClaimsSet claims) {
    return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
  }

  private JwtClaimsSet.Builder validClaims() {
    return JwtClaimsSet.builder()
        .subject("guardian-id")
        .issuer(ISSUER)
        .audience(List.of(AUDIENCE))
        .issuedAt(NOW)
        .expiresAt(NOW.plusSeconds(900));
  }

  private String hs384Token(JwtClaimsSet claims) throws Exception {
    SignedJWT token =
        new SignedJWT(
            new JWSHeader(JWSAlgorithm.HS384),
            new JWTClaimsSet.Builder()
                .subject(claims.getSubject())
                .issuer(claims.getClaimAsString("iss"))
                .audience(claims.getAudience())
                .issueTime(Date.from(claims.getIssuedAt()))
                .expirationTime(Date.from(claims.getExpiresAt()))
                .build());
    token.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
    return token.serialize();
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(JwtProperties.class)
  @Import(JwtConfig.class)
  static class JwtTestConfiguration {

    @Bean
    Clock clock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }
}

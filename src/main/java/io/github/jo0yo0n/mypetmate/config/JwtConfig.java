package io.github.jo0yo0n.mypetmate.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {

  private static final Duration CLOCK_SKEW = Duration.ZERO;

  @Bean
  JwtEncoder jwtEncoder(JwtProperties jwtProperties) {
    NimbusJwtEncoder encoder =
        new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(jwtProperties)));

    return parameters -> {
      if (parameters.getJwsHeader() != null
          && !MacAlgorithm.HS256.equals(parameters.getJwsHeader().getAlgorithm())) {
        throw new JwtEncodingException("Only HS256 is supported for access tokens");
      }
      JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
      return encoder.encode(JwtEncoderParameters.from(header, parameters.getClaims()));
    };
  }

  @Bean
  JwtDecoder jwtDecoder(JwtProperties jwtProperties, Clock clock) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(secretKey(jwtProperties))
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    decoder.setJwtValidator(jwtValidator(jwtProperties, clock));
    return decoder;
  }

  private OAuth2TokenValidator<Jwt> jwtValidator(JwtProperties jwtProperties, Clock clock) {
    JwtTimestampValidator timestampValidator = new JwtTimestampValidator(CLOCK_SKEW);
    timestampValidator.setClock(clock);
    JwtClaimValidator<String> issuerValidator =
        new JwtClaimValidator<>("iss", jwtProperties.issuer()::equals);
    JwtClaimValidator<List<String>> audienceValidator =
        new JwtClaimValidator<>(
            "aud", audience -> audience != null && audience.contains(jwtProperties.audience()));

    return new DelegatingOAuth2TokenValidator<>(
        timestampValidator, issuerValidator, audienceValidator);
  }

  private SecretKey secretKey(JwtProperties jwtProperties) {
    return new SecretKeySpec(jwtProperties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }
}

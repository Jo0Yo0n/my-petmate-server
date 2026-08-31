package io.github.jo0yo0n.mypetmate.config;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer, String audience) {

  private static final int MIN_SECRET_BYTES = 32;

  public JwtProperties {
    if (secret == null || secret.isBlank()) {
      throw new IllegalArgumentException("app.jwt.secret must be provided");
    }
    if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
      throw new IllegalArgumentException(
          "app.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes for HS256");
    }
    if (issuer == null || issuer.isBlank()) {
      throw new IllegalArgumentException("app.jwt.issuer must be provided");
    }
    if (audience == null || audience.isBlank()) {
      throw new IllegalArgumentException("app.jwt.audience must be provided");
    }
  }
}

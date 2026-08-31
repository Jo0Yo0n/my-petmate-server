package io.github.jo0yo0n.mypetmate.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.token")
public record TokenProperties(Duration accessTokenTtl, Duration refreshTokenTtl, String tokenType) {

  public TokenProperties {
    if (accessTokenTtl == null || accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
      throw new IllegalArgumentException("app.token.access-token-ttl must be positive");
    }
    if (refreshTokenTtl == null || refreshTokenTtl.isZero() || refreshTokenTtl.isNegative()) {
      throw new IllegalArgumentException("app.token.refresh-token-ttl must be positive");
    }
    if (tokenType == null || tokenType.isBlank()) {
      throw new IllegalArgumentException("app.token.token-type must be provided");
    }
  }
}

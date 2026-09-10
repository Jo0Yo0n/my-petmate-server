package io.github.jo0yo0n.mypetmate.auth;

import io.github.jo0yo0n.mypetmate.config.JwtProperties;
import io.github.jo0yo0n.mypetmate.config.TokenProperties;
import io.github.jo0yo0n.mypetmate.guardian.persistence.Guardian;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenIssuer {

  private final JwtEncoder jwtEncoder;
  private final JwtProperties jwtProperties;
  private final TokenProperties tokenProperties;

  public AccessTokenIssuer(
      JwtEncoder jwtEncoder, JwtProperties jwtProperties, TokenProperties tokenProperties) {
    this.jwtEncoder = jwtEncoder;
    this.jwtProperties = jwtProperties;
    this.tokenProperties = tokenProperties;
  }

  String issue(Guardian guardian, Instant now) {

    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(guardian.getId().toString())
            .issuer(jwtProperties.issuer())
            .audience(List.of(jwtProperties.audience()))
            .issuedAt(now)
            .expiresAt(now.plus(tokenProperties.accessTokenTtl()))
            .id(UUID.randomUUID().toString())
            .build();

    return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
  }
}

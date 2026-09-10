package io.github.jo0yo0n.mypetmate.auth;

import io.github.jo0yo0n.mypetmate.auth.dto.AuthResponse;
import io.github.jo0yo0n.mypetmate.auth.dto.SignupRequest;
import io.github.jo0yo0n.mypetmate.auth.exception.EmailAlreadyExistsException;
import io.github.jo0yo0n.mypetmate.config.JwtProperties;
import io.github.jo0yo0n.mypetmate.config.TokenProperties;
import io.github.jo0yo0n.mypetmate.guardian.domain.GuardianStatus;
import io.github.jo0yo0n.mypetmate.guardian.dto.GuardianResponse;
import io.github.jo0yo0n.mypetmate.guardian.persistence.Guardian;
import io.github.jo0yo0n.mypetmate.guardian.persistence.GuardianRepository;
import io.github.jo0yo0n.mypetmate.guardian.persistence.RefreshToken;
import io.github.jo0yo0n.mypetmate.guardian.persistence.RefreshTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final Clock clock;
  private final JwtEncoder jwtEncoder;
  private final JwtProperties jwtProperties;
  private final TokenProperties tokenProperties;
  private final GuardianRepository guardianRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final RefreshTokenGenerator refreshTokenGenerator;
  private final PasswordEncoder passwordEncoder;

  public AuthService(
      Clock clock,
      JwtEncoder jwtEncoder,
      JwtProperties jwtProperties,
      TokenProperties tokenProperties,
      GuardianRepository guardianRepository,
      RefreshTokenRepository refreshTokenRepository,
      RefreshTokenGenerator refreshTokenGenerator,
      PasswordEncoder passwordEncoder) {
    this.clock = clock;
    this.jwtEncoder = jwtEncoder;
    this.jwtProperties = jwtProperties;
    this.tokenProperties = tokenProperties;
    this.guardianRepository = guardianRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.refreshTokenGenerator = refreshTokenGenerator;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  AuthResponse signup(SignupRequest signupRequest) {

    if (guardianRepository.existsByEmail(signupRequest.email())) {
      throw new EmailAlreadyExistsException();
    }

    Instant now = clock.instant();
    Guardian guardian;

    // 동시 조회로 인한 데이터 무결성 위반을 처리하기 위해 try-catch 블록 사용
    try {
      guardian =
          guardianRepository.saveAndFlush(
              new Guardian(
                  UUID.randomUUID(),
                  signupRequest.email(),
                  passwordEncoder.encode(signupRequest.password()),
                  signupRequest.profileType(),
                  signupRequest.gender(),
                  signupRequest.identityVisibility(),
                  GuardianStatus.ACTIVE,
                  now,
                  now));
    } catch (DataIntegrityViolationException exception) {
      Throwable cause = exception;

      while (cause != null) {
        if (cause instanceof ConstraintViolationException violation) {
          if ("uk_guardian_email_lower".equals(violation.getConstraintName())) {
            throw new EmailAlreadyExistsException();
          }
        }
        cause = cause.getCause();
      }
      throw exception;
    }

    String refreshToken = refreshTokenGenerator.generate();
    refreshTokenRepository.save(
        new RefreshToken(
            UUID.randomUUID(),
            guardian,
            refreshTokenGenerator.hash(refreshToken),
            now.plus(tokenProperties.refreshTokenTtl()),
            null,
            now));

    String accessToken = issueAccessToken(guardian);

    return new AuthResponse(
        accessToken,
        refreshToken,
        tokenProperties.tokenType(),
        Math.toIntExact(tokenProperties.accessTokenTtl().toSeconds()),
        Math.toIntExact(tokenProperties.refreshTokenTtl().toSeconds()),
        GuardianResponse.from(guardian));
  }

  String issueAccessToken(Guardian guardian) {
    Instant now = clock.instant();

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

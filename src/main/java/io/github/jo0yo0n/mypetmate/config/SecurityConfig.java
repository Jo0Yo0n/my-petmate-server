package io.github.jo0yo0n.mypetmate.config;

import io.github.jo0yo0n.mypetmate.auth.exception.AccountStatusAuthenticationException;
import io.github.jo0yo0n.mypetmate.guardian.domain.GuardianStatus;
import io.github.jo0yo0n.mypetmate.guardian.persistence.Guardian;
import io.github.jo0yo0n.mypetmate.guardian.persistence.GuardianRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
public class SecurityConfig {

  private final GuardianRepository guardianRepository;

  public SecurityConfig(GuardianRepository guardianRepository) {
    this.guardianRepository = guardianRepository;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      AuthenticationEntryPoint authenticationEntryPoint,
      AccessDeniedHandler accessDeniedHandler)
      throws Exception {

    http.formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        HttpMethod.POST,
                        "/api/auth/signup",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/logout")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(this::convertJwt)));

    return http.build();
  }

  private AbstractAuthenticationToken convertJwt(Jwt jwt) {

    String subject = jwt.getSubject();
    if (subject == null || subject.isBlank()) {
      throw new InvalidBearerTokenException("Invalid subject");
    }

    UUID guardianId;
    try {
      guardianId = UUID.fromString(subject);
    } catch (IllegalArgumentException exception) {
      throw new InvalidBearerTokenException("Invalid subject", exception);
    }

    Guardian guardian =
        guardianRepository
            .findById(guardianId)
            .orElseThrow(() -> new InvalidBearerTokenException("Invalid guardian"));

    if (guardian.getStatus() == GuardianStatus.WITHDRAWN
        || guardian.getStatus() == GuardianStatus.TEMPORARILY_RESTRICTED) {
      throw new AccountStatusAuthenticationException(guardian.getStatus());
    }

    return UsernamePasswordAuthenticationToken.authenticated(guardian.getId(), null, List.of());
  }
}

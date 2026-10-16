package io.github.jo0yo0n.mypetmate.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jo0yo0n.mypetmate.guardian.domain.*;
import io.github.jo0yo0n.mypetmate.guardian.persistence.Guardian;
import io.github.jo0yo0n.mypetmate.guardian.persistence.GuardianRepository;
import io.github.jo0yo0n.mypetmate.support.PostgreSqlIntegrationTestSupport;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityFilterChainIntegrationTest.TestConfig.class)
class SecurityFilterChainIntegrationTest extends PostgreSqlIntegrationTestSupport {
  private static final Instant NOW = Instant.parse("2026-10-12T00:00:00Z");
  @Autowired MockMvc mvc;
  @Autowired JwtEncoder encoder;
  @Autowired JwtProperties properties;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;
  @MockitoSpyBean GuardianRepository guardians;

  @ParameterizedTest
  @ValueSource(strings = {"signup", "login", "refresh", "logout"})
  @DisplayName("[M1-SEC-01] allowsPublicAuthRequestsWithoutAccessToken")
  void allowsPublicAuthRequestsWithoutAccessToken(String endpoint) throws Exception {

    // 400 응답은 access token 없이 Security를 통과해 MVC 입력 검증에 도달했음을 확인한다.
    assertProblemResponse(
        post("/api/auth/" + endpoint).contentType(MediaType.APPLICATION_JSON).content("{}"),
        400,
        "VALIDATION_FAILED");
  }

  @Test
  @DisplayName("[M1-SEC-02] returnsProblemDetailWhenAccessTokenIsMissing")
  void returnsProblemDetailWhenAccessTokenIsMissing() throws Exception {
    assertProblemResponse(get("/api/guardians/me"), 401, "AUTH_TOKEN_INVALID");
    assertProblemResponse(get("/api/auth/login"), 401, "AUTH_TOKEN_INVALID");
    assertProblemResponse(
        get("/api/guardians/me").header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"),
        401,
        "AUTH_TOKEN_INVALID");
  }

  @Test
  @DisplayName("[M1-SEC-03A] rejectsTamperedAccessToken")
  void rejectsTamperedAccessToken() throws Exception {
    String token = token(UUID.randomUUID().toString(), NOW.plusSeconds(900));
    int start = token.lastIndexOf('.') + 1;
    String tampered =
        token.substring(0, start)
            + (token.charAt(start) == 'A' ? 'B' : 'A')
            + token.substring(start + 1);
    assertProblemResponse(bearer("/test/secured", tampered), 401, "AUTH_TOKEN_INVALID");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "not-a-uuid"})
  @DisplayName("[M1-SEC-03B] rejectsMalformedGuardianSubject")
  void rejectsMalformedGuardianSubject(String subject) throws Exception {
    assertProblemResponse(
        bearer("/test/secured", token(subject, NOW.plusSeconds(900))), 401, "AUTH_TOKEN_INVALID");
  }

  @Test
  @DisplayName("[M1-SEC-04] returnsExpiredTokenProblemDetail")
  void returnsExpiredTokenProblemDetail() throws Exception {
    for (Instant expiration : new Instant[] {NOW.minusSeconds(1), NOW}) {
      assertProblemResponse(
          bearer("/test/secured", token(UUID.randomUUID().toString(), expiration)),
          401,
          "AUTH_TOKEN_EXPIRED");
    }
  }

  @Test
  @DisplayName("[M1-SEC-05] restoresCurrentGuardianFromValidToken")
  void restoresCurrentGuardianFromValidToken() throws Exception {
    UUID id = saveGuardian(GuardianStatus.ACTIVE);
    String token = token(id.toString(), NOW.plusSeconds(900));
    mvc.perform(bearer("/test/secured", token))
        .andExpect(status().isOk())
        .andExpect(content().string(id.toString()));
    jdbc.update("UPDATE guardian SET status = ? WHERE id = ?", "temporarily_restricted", id);
    assertProblemResponse(bearer("/test/secured", token), 403, "ACCOUNT_RESTRICTED");
  }

  @Test
  @DisplayName("[M1-SEC-06] rejectsMissingGuardian")
  void rejectsMissingGuardian() throws Exception {
    assertProblemResponse(
        bearer("/test/secured", token(UUID.randomUUID().toString(), NOW.plusSeconds(900))),
        401,
        "AUTH_TOKEN_INVALID");
  }

  @Test
  @DisplayName("[M1-SEC-07] returnsProblemDetailWhenAccessIsDenied")
  void returnsProblemDetailWhenAccessIsDenied() throws Exception {
    assertProblemResponse(
        bearer("/test/denied", token(UUID.randomUUID().toString(), NOW.plusSeconds(900))),
        403,
        "FORBIDDEN");
  }

  @Test
  @DisplayName("[M1-SEC-09] authenticatesWithoutCreatingHttpSession")
  void authenticatesWithoutCreatingHttpSession() throws Exception {
    UUID id = saveGuardian(GuardianStatus.ACTIVE);
    String token = token(id.toString(), NOW.plusSeconds(900));
    for (int i = 0; i < 2; i++) {
      var result =
          mvc.perform(bearer("/test/secured", token)).andExpect(status().isOk()).andReturn();
      assertThat(result.getRequest().getSession(false)).isNull();
      assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
    }
  }

  @ParameterizedTest
  @CsvSource({
    "TEMPORARILY_RESTRICTED, ACCOUNT_RESTRICTED, 일시 정지된 계정은 이용할 수 없습니다.",
    "WITHDRAWN, ACCOUNT_WITHDRAWN, 탈퇴한 계정은 이용할 수 없습니다."
  })
  @DisplayName("[M1-SEC-10] rejectsInactiveGuardianWithAccountStatusError")
  void rejectsInactiveGuardianWithAccountStatusError(
      GuardianStatus status, String code, String detail) throws Exception {
    UUID id = saveGuardian(status);
    MvcResult result =
        assertProblemResponse(
            bearer("/test/secured", token(id.toString(), NOW.plusSeconds(900))), 403, code);
    assertThat(mapper.readTree(result.getResponse().getContentAsString()).path("detail").asText())
        .isEqualTo(detail);
    assertThat(result.getRequest().getSession(false)).isNull();

    assertProblemResponse(
        bearer("/test/secured", token(id.toString(), NOW.minusSeconds(1))),
        401,
        "AUTH_TOKEN_EXPIRED");

    String validToken = token(id.toString(), NOW.plusSeconds(900));
    int signatureStart = validToken.lastIndexOf('.') + 1;
    String tampered =
        validToken.substring(0, signatureStart)
            + (validToken.charAt(signatureStart) == 'A' ? 'B' : 'A')
            + validToken.substring(signatureStart + 1);
    assertProblemResponse(bearer("/test/secured", tampered), 401, "AUTH_TOKEN_INVALID");
  }

  @Test
  @DisplayName("[M1-DBFILTER-04] handlesRepositoryFailureInSecurityChain")
  void handlesRepositoryFailureInSecurityChain() throws Exception {
    UUID id = UUID.randomUUID();
    doThrow(new DataAccessResourceFailureException("private database detail"))
        .when(guardians)
        .findById(id);
    try {
      assertProblemResponse(
          bearer("/test/secured", token(id.toString(), NOW.plusSeconds(900))),
          500,
          "INTERNAL_SERVER_ERROR");
    } finally {
      reset(guardians);
    }
  }

  @Test
  @ExtendWith(OutputCaptureExtension.class)
  @DisplayName("[M1-DBFILTER-05] handlesTransactionCreationFailureInSecurityChain")
  void handlesTransactionCreationFailureInSecurityChain(CapturedOutput output) throws Exception {
    UUID id = UUID.randomUUID();
    doThrow(
            new CannotCreateTransactionException(
                "Could not open JPA EntityManager for transaction",
                new DataAccessResourceFailureException("private database detail")))
        .when(guardians)
        .findById(id);
    try {
      assertProblemResponse(
          bearer("/test/secured", token(id.toString(), NOW.plusSeconds(900))),
          500,
          "INTERNAL_SERVER_ERROR");
    } finally {
      reset(guardians);
    }
  }

  private UUID saveGuardian(GuardianStatus status) {
    UUID id = UUID.randomUUID();
    guardians.saveAndFlush(
        new Guardian(
            id,
            id + "@example.com",
            "fixture-hash",
            ProfileType.INDIVIDUAL,
            Gender.MALE,
            IdentityVisibility.PRIVATE,
            status,
            NOW,
            NOW));
    return id;
  }

  private String token(String subject, Instant expiresAt) {
    var claims =
        JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .audience(List.of(properties.audience()))
            .issuedAt(NOW.minusSeconds(900))
            .expiresAt(expiresAt);
    if (subject != null) claims.subject(subject);
    return encoder.encode(JwtEncoderParameters.from(claims.build())).getTokenValue();
  }

  private MockHttpServletRequestBuilder bearer(String path, String token) {
    return get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
  }

  private MvcResult assertProblemResponse(
      MockHttpServletRequestBuilder request, int status, String code) throws Exception {
    var result =
        mvc.perform(request)
            .andExpect(status().is(status))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(status))
            .andExpect(jsonPath("$.code").value(code))
            .andReturn();
    var body = mapper.readTree(result.getResponse().getContentAsString());
    var error = io.github.jo0yo0n.mypetmate.error.ErrorCode.valueOf(code);
    assertThat(body.path("type").asText()).isEqualTo(error.getType().toString());
    assertThat(body.path("title").asText()).isEqualTo(error.getTitle());
    assertThat(body.path("detail").asText()).isNotBlank();
    assertThat(body.path("instance").asText()).isEqualTo(result.getRequest().getRequestURI());
    String requestId = result.getResponse().getHeader("My-Petmate-Request-Id");
    assertThat(requestId).matches("^req-[0-7][0-9A-HJKMNP-TV-Z]{25}$");
    assertThat(body.path("requestId").asText()).isEqualTo(requestId);
    if (status != 400) assertThat(body.has("fieldErrors")).isFalse();
    assertThat(result.getResponse().getContentAsString())
        .doesNotContain(
            "private database detail",
            "password_hash",
            "DataAccessResourceFailureException",
            "CannotCreateTransactionException");
    return result;
  }

  @TestConfiguration
  @Import(TestController.class)
  static class TestConfig {
    @Bean
    @Primary
    Clock testClock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    // M1 has no authority-protected endpoint yet; exercise the production handlers with a deny
    // rule.
    @Bean
    @Order(0)
    SecurityFilterChain deniedChain(
        HttpSecurity http, AuthenticationEntryPoint entryPoint, AccessDeniedHandler deniedHandler)
        throws Exception {
      return http.securityMatcher("/test/denied")
          .sessionManagement(
              session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(auth -> auth.anyRequest().denyAll())
          .exceptionHandling(
              errors ->
                  errors.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
          .oauth2ResourceServer(
              oauth ->
                  oauth
                      .authenticationEntryPoint(entryPoint)
                      .accessDeniedHandler(deniedHandler)
                      .jwt(jwt -> {}))
          .build();
    }
  }

  @RestController
  static class TestController {
    @GetMapping("/test/secured")
    String secured(@AuthenticationPrincipal UUID id) {
      return id.toString();
    }

    @GetMapping("/test/denied")
    String denied() {
      throw new AssertionError("Denied controller must not execute");
    }
  }
}

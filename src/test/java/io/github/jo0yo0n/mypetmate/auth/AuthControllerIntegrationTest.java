package io.github.jo0yo0n.mypetmate.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.jo0yo0n.mypetmate.support.PostgreSqlIntegrationTestSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest extends PostgreSqlIntegrationTestSupport {
  private static final String SIGNUP_PATH = "/api/auth/signup";
  private static final String LOGIN_PATH = "/api/auth/login";
  private static final String REFRESH_PATH = "/api/auth/refresh";
  private static final String LOGOUT_PATH = "/api/auth/logout";
  private static final String EMAIL = "guardian@example.com";
  private static final String PASSWORD = "StrongPass123!";
  private static final String REQUEST_ID = "My-Petmate-Request-Id";

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;

  @ParameterizedTest
  @CsvSource({
    "individual,female,public",
    "couple,,private",
    "family,,public",
    "couple,female,public",
    "couple,male,private",
    "family,female,private",
    "family,male,public",
  })
  @DisplayName("[M1-API-SU-01] returnsCreatedAuthResponseForSignup")
  void returnsCreatedAuthResponseForSignup(String profileType, String gender, String visibility)
      throws Exception {
    ObjectNode request = request(profileType, gender, visibility);
    MvcResult result = signup(request).andExpect(status().isCreated()).andReturn();
    assertThat(result.getResponse().getHeader(REQUEST_ID)).isNotBlank();
    JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
    assertThat(
            MediaType.parseMediaType(Objects.requireNonNull(result.getResponse().getContentType()))
                .isCompatibleWith(MediaType.APPLICATION_JSON))
        .isTrue();
    assertThat(fields(body))
        .containsExactlyInAnyOrder(
            "accessToken",
            "refreshToken",
            "tokenType",
            "expiresIn",
            "refreshExpiresIn",
            "guardian");
    assertThat(body.path("accessToken").isTextual()).isTrue();
    assertThat(body.path("refreshToken").isTextual()).isTrue();
    assertThat(body.path("tokenType").isTextual()).isTrue();
    assertThat(body.path("expiresIn").isIntegralNumber()).isTrue();
    assertThat(body.path("refreshExpiresIn").isIntegralNumber()).isTrue();
    assertThat(body.path("guardian").isObject()).isTrue();

    JsonNode guardian = body.path("guardian");
    List<String> expectedFields =
        List.of("id", "email", "profileType", "gender", "identityVisibility", "status");
    assertThat(fields(guardian)).containsExactlyInAnyOrderElementsOf(expectedFields);
    assertThat(guardian.path("email").asText()).isEqualTo(EMAIL);
    assertThat(guardian.path("profileType").asText()).isEqualTo(profileType);
    assertThat(guardian.path("identityVisibility").asText()).isEqualTo(visibility);
    assertThat(guardian.path("status").asText()).isEqualTo("active");
    String expectedGender = "individual".equals(profileType) ? gender : null;
    if (expectedGender == null) {
      assertThat(guardian.path("gender").isNull()).isTrue();
    } else {
      assertThat(guardian.path("gender").asText()).isEqualTo(expectedGender);
    }

    assertThat(count("guardian")).isEqualTo(1);
    assertThat(count("refresh_token")).isEqualTo(1);

    UUID id = UUID.fromString(guardian.path("id").asText());
    assertThat(jdbc.queryForObject("SELECT status FROM guardian WHERE id = ?", String.class, id))
        .isEqualTo("active");
    assertThat(jdbc.queryForObject("SELECT gender FROM guardian WHERE id = ?", String.class, id))
        .isEqualTo(expectedGender);
  }

  @Test
  @DisplayName("[M1-API-SU-02A] rejectsInvalidSignupWithoutCreatingGuardian")
  void rejectsInvalidSignupWithoutCreatingGuardian() throws Exception {
    ObjectNode request = request("individual", null, "public");
    long guardians = count("guardian");
    long refreshTokens = count("refresh_token");
    JsonNode error =
        assertProblemResponse(
            SIGNUP_PATH, request, 400, "VALIDATION_FAILED", "validation-failed", "입력값 검증 실패");
    assertThat(error.path("fieldErrors").isArray()).isTrue();
    assertThat(error.path("fieldErrors")).isNotEmpty();
    List<String> errorFields = new ArrayList<>();
    for (JsonNode field : error.path("fieldErrors")) {
      errorFields.add(field.path("field").asText());
      assertThat(field.path("reason").asText()).isNotBlank();
    }
    assertThat(errorFields).contains("gender");
    assertCountUnchanged(guardians, refreshTokens);
  }

  @Test
  @DisplayName("[M1-API-SU-02B] rejectsUnknownSignupFieldWithoutCreatingGuardian")
  void rejectsUnknownSignupFieldWithoutCreatingGuardian() throws Exception {
    ObjectNode request = request("individual", "female", "public");
    request.put("unknownField", "unexpected");
    long guardians = count("guardian");
    long refreshTokens = count("refresh_token");
    JsonNode error =
        assertProblemResponse(
            SIGNUP_PATH, request, 400, "MALFORMED_REQUEST", "malformed-request", "잘못된 요청 형식");
    assertThat(error.has("fieldErrors")).isFalse();
    assertCountUnchanged(guardians, refreshTokens);
  }

  @Test
  @DisplayName("[M1-API-SU-03] returnsEmailConflictForDuplicateSignup")
  void returnsEmailConflictForDuplicateSignup() throws Exception {
    ObjectNode firstRequest = request("individual", "female", "public");
    firstRequest.put("email", EMAIL);
    signup(firstRequest).andExpect(status().isCreated());
    long guardians = count("guardian");
    long refreshTokens = count("refresh_token");
    JsonNode error =
        assertProblemResponse(
            SIGNUP_PATH,
            request("individual", "female", "public"),
            409,
            "EMAIL_ALREADY_EXISTS",
            "email-already-exists",
            "이미 가입된 이메일");
    assertThat(error.has("fieldErrors")).isFalse();
    assertCountUnchanged(guardians, refreshTokens);
  }

  @Test
  @DisplayName("[M1-API-LI-01] returnsAuthResponseForValidLogin")
  void returnsAuthResponseForValidLogin() throws Exception {
    JsonNode signedUp =
        mapper.readTree(
            signup(request("individual", "female", "public"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    long refreshTokens = count("refresh_token");

    MvcResult result =
        login(loginRequest("  Guardian@Example.COM  ", PASSWORD))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andReturn();

    assertThat(result.getResponse().getHeader(REQUEST_ID)).isNotBlank();
    JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
    assertThat(fields(body))
        .containsExactlyInAnyOrder(
            "accessToken",
            "refreshToken",
            "tokenType",
            "expiresIn",
            "refreshExpiresIn",
            "guardian");
    assertThat(body.path("accessToken").isTextual()).isTrue();
    assertThat(body.path("refreshToken").isTextual()).isTrue();
    assertThat(body.path("tokenType").isTextual()).isTrue();
    assertThat(body.path("expiresIn").isIntegralNumber()).isTrue();
    assertThat(body.path("refreshExpiresIn").isIntegralNumber()).isTrue();
    assertThat(body.path("guardian").isObject()).isTrue();
    assertThat(body.path("guardian")).isEqualTo(signedUp.path("guardian"));

    assertThat(count("refresh_token")).isEqualTo(refreshTokens + 1);
  }

  @Test
  @DisplayName("[M1-API-LI-02] returnsSameErrorForInvalidLoginCredentials")
  void returnsSameErrorForInvalidLoginCredentials() throws Exception {
    signup(request("individual", "female", "public")).andExpect(status().isCreated());

    for (String email : List.of("missing@example.com", EMAIL)) {
      // 유효 길이지만 signup 복잡도에는 맞지 않는 비밀번호도 인증 단계까지 전달
      JsonNode error =
          assertProblemResponse(
              LOGIN_PATH,
              loginRequest(email, "simple"),
              401,
              "AUTH_INVALID_CREDENTIALS",
              "invalid-credentials",
              "유효하지 않은 로그인 정보");
      assertThat(error.has("fieldErrors")).isFalse();
    }
  }

  @ParameterizedTest
  @CsvSource({
    "temporarily_restricted,ACCOUNT_RESTRICTED,account-restricted,정지된 계정",
    "withdrawn,ACCOUNT_WITHDRAWN,account-withdrawn,탈퇴한 계정"
  })
  @DisplayName("[M1-API-LI-03] returnsAccountStatusErrorForValidLogin")
  void returnsAccountStatusErrorForValidLogin(
      String accountStatus, String code, String type, String title) throws Exception {
    signup(request("individual", "female", "public")).andExpect(status().isCreated());
    jdbc.update("UPDATE guardian SET status = ? WHERE email = ?", accountStatus, EMAIL);
    long refreshTokens = count("refresh_token");

    JsonNode error =
        assertProblemResponse(LOGIN_PATH, loginRequest(EMAIL, PASSWORD), 403, code, type, title);

    assertThat(error.has("fieldErrors")).isFalse();
    assertThat(error.has("accessToken")).isFalse();
    assertThat(error.has("refreshToken")).isFalse();
    assertThat(count("refresh_token")).isEqualTo(refreshTokens);
  }

  @Test
  @DisplayName("[M1-API-RF-01] rotatesRefreshTokenAndRejectsReuse")
  void rotatesRefreshTokenAndRejectsReuse() throws Exception {
    JsonNode signedUp =
        mapper.readTree(
            signup(request("individual", "female", "public"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    String previousToken = signedUp.path("refreshToken").asText();

    MvcResult result =
        refresh(refreshRequest(previousToken))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andReturn();

    assertThat(result.getResponse().getHeader(REQUEST_ID)).isNotBlank();
    JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
    assertThat(fields(body))
        .containsExactlyInAnyOrder(
            "accessToken", "refreshToken", "tokenType", "expiresIn", "refreshExpiresIn");
    assertThat(body.path("accessToken").isTextual()).isTrue();
    assertThat(body.path("refreshToken").isTextual()).isTrue();
    assertThat(body.path("tokenType").isTextual()).isTrue();
    assertThat(body.path("expiresIn").isIntegralNumber()).isTrue();
    assertThat(body.path("refreshExpiresIn").isIntegralNumber()).isTrue();
    assertThat(body.path("refreshToken").asText()).isNotBlank().isNotEqualTo(previousToken);

    JsonNode error =
        assertProblemResponse(
            REFRESH_PATH,
            refreshRequest(previousToken),
            401,
            "AUTH_REFRESH_INVALID",
            "refresh-token-invalid",
            "유효하지 않은 refresh token");
    assertThat(error.has("fieldErrors")).isFalse();
  }

  @Test
  @DisplayName("[M1-API-RF-02A] returnsValidationErrorForMalformedRefreshToken")
  void returnsValidationErrorForMalformedRefreshToken() throws Exception {
    JsonNode error =
        assertProblemResponse(
            REFRESH_PATH,
            refreshRequest("invalid-token"),
            400,
            "VALIDATION_FAILED",
            "validation-failed",
            "입력값 검증 실패");
    assertThat(error.path("fieldErrors").isArray()).isTrue();
    assertThat(error.path("fieldErrors")).isNotEmpty();
    List<String> errorFields = new ArrayList<>();
    for (JsonNode field : error.path("fieldErrors")) {
      errorFields.add(field.path("field").asText());
      assertThat(field.path("reason").asText()).isNotBlank();
    }
    assertThat(errorFields).contains("refreshToken");
  }

  @Test
  @DisplayName("[M1-API-RF-02B] returnsInvalidRefreshForUnknownToken")
  void returnsInvalidRefreshForUnknownToken() throws Exception {
    JsonNode error =
        assertProblemResponse(
            REFRESH_PATH,
            refreshRequest("a".repeat(43)),
            401,
            "AUTH_REFRESH_INVALID",
            "refresh-token-invalid",
            "유효하지 않은 refresh token");
    assertThat(error.has("fieldErrors")).isFalse();
  }

  @ParameterizedTest
  @CsvSource({
    "temporarily_restricted,ACCOUNT_RESTRICTED,account-restricted,정지된 계정",
    "withdrawn,ACCOUNT_WITHDRAWN,account-withdrawn,탈퇴한 계정"
  })
  @DisplayName("[M1-API-RF-03] returnsAccountStatusErrorForValidRefresh")
  void returnsAccountStatusErrorForValidRefresh(
      String accountStatus, String code, String type, String title) throws Exception {
    JsonNode signedUp =
        mapper.readTree(
            signup(request("individual", "female", "public"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    jdbc.update("UPDATE guardian SET status = ? WHERE email = ?", accountStatus, EMAIL);
    var tokensBefore = jdbc.queryForList("SELECT * FROM refresh_token ORDER BY id");

    JsonNode error =
        assertProblemResponse(
            REFRESH_PATH,
            refreshRequest(signedUp.path("refreshToken").asText()),
            403,
            code,
            type,
            title);

    assertThat(error.has("fieldErrors")).isFalse();
    assertThat(error.has("accessToken")).isFalse();
    assertThat(error.has("refreshToken")).isFalse();
    assertThat(jdbc.queryForList("SELECT * FROM refresh_token ORDER BY id"))
        .isEqualTo(tokensBefore);
  }

  @Test
  @DisplayName("[M1-API-LO-01] returnsNoContentForRepeatedLogout")
  void returnsNoContentForRepeatedLogout() throws Exception {
    JsonNode signedUp =
        mapper.readTree(
            signup(request("individual", "female", "public"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    String token = signedUp.path("refreshToken").asText();

    for (String logoutToken : List.of(token, token, "a".repeat(43))) {
      MvcResult result =
          logout(refreshRequest(logoutToken))
              .andExpect(status().isNoContent())
              .andExpect(content().string(""))
              .andReturn();
      assertThat(result.getResponse().getHeader(REQUEST_ID)).isNotBlank();
    }

    JsonNode error =
        assertProblemResponse(
            REFRESH_PATH,
            refreshRequest(token),
            401,
            "AUTH_REFRESH_INVALID",
            "refresh-token-invalid",
            "유효하지 않은 refresh token");
    assertThat(error.has("fieldErrors")).isFalse();
  }

  private ObjectNode request(String type, String gender, String visibility) {
    ObjectNode request = mapper.createObjectNode();
    request.put("email", "  Guardian@Example.COM  ");
    request.put("password", PASSWORD);
    request.put("profileType", type);
    if (gender != null) request.put("gender", gender);
    request.put("identityVisibility", visibility);
    return request;
  }

  private ResultActions signup(ObjectNode request) throws Exception {
    return mvc.perform(
        post(SIGNUP_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(request)));
  }

  private ObjectNode loginRequest(String email, String password) {
    return mapper.createObjectNode().put("email", email).put("password", password);
  }

  private ResultActions login(ObjectNode request) throws Exception {
    return mvc.perform(
        post(LOGIN_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(request)));
  }

  private ObjectNode refreshRequest(String token) {
    return mapper.createObjectNode().put("refreshToken", token);
  }

  private ResultActions refresh(ObjectNode request) throws Exception {
    return mvc.perform(
        post(REFRESH_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(request)));
  }

  private ResultActions logout(ObjectNode request) throws Exception {
    return mvc.perform(
        post(LOGOUT_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(request)));
  }

  private JsonNode assertProblemResponse(
      String path, ObjectNode request, int status, String code, String type, String title)
      throws Exception {
    MvcResult result =
        mvc.perform(
                post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(request)))
            .andExpect(status().is(status))
            .andReturn();
    assertThat(
            MediaType.parseMediaType(Objects.requireNonNull(result.getResponse().getContentType()))
                .isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .isTrue();
    JsonNode error = mapper.readTree(result.getResponse().getContentAsString());
    assertThat(error.path("status").asInt()).isEqualTo(status);
    assertThat(error.path("code").asText()).isEqualTo(code);
    assertThat(error.path("type").asText()).isEqualTo("urn:my-petmate:problem:" + type);
    assertThat(error.path("title").asText()).isEqualTo(title);
    assertThat(error.path("detail").asText()).isNotBlank();
    assertThat(error.path("instance").asText()).isEqualTo(path);
    String requestId = result.getResponse().getHeader(REQUEST_ID);
    assertThat(requestId).matches("^req-[0-7][0-9A-HJKMNP-TV-Z]{25}$");
    assertThat(error.path("requestId").asText()).isNotBlank().isEqualTo(requestId);
    return error;
  }

  private List<String> fields(JsonNode node) {
    List<String> fields = new ArrayList<>();
    node.fieldNames().forEachRemaining(fields::add);
    return fields;
  }

  private long count(String table) {
    return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
  }

  private void assertCountUnchanged(long guardians, long refreshTokens) {
    assertThat(count("guardian")).isEqualTo(guardians);
    assertThat(count("refresh_token")).isEqualTo(refreshTokens);
  }
}

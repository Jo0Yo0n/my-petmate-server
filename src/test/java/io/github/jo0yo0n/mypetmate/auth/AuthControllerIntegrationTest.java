package io.github.jo0yo0n.mypetmate.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
  private static final String PATH = "/api/auth/signup";
  private static final String EMAIL = "guardian@example.com";
  private static final String PASSWORD = "StrongPass123!";
  private static final String REQUEST_ID = "My-Petmate-Request-Id";

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;

  @ParameterizedTest
  @CsvSource({"individual,female,public", "couple,,private", "family,,public"})
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
        new ArrayList<>(List.of("id", "email", "profileType", "identityVisibility", "status"));
    if (gender != null) expectedFields.add("gender");
    assertThat(fields(guardian)).containsExactlyInAnyOrderElementsOf(expectedFields);
    assertThat(guardian.path("email").asText()).isEqualTo(EMAIL);
    assertThat(guardian.path("profileType").asText()).isEqualTo(profileType);
    assertThat(guardian.path("identityVisibility").asText()).isEqualTo(visibility);
    assertThat(guardian.path("status").asText()).isEqualTo("active");
    if (gender != null) assertThat(guardian.path("gender").asText()).isEqualTo(gender);

    assertThat(count("guardian")).isEqualTo(1);
    assertThat(count("refresh_token")).isEqualTo(1);

    UUID id = UUID.fromString(guardian.path("id").asText());
    assertThat(jdbc.queryForObject("SELECT status FROM guardian WHERE id = ?", String.class, id))
        .isEqualTo("active");
  }

  @Test
  @DisplayName("[M1-API-SU-02A] rejectsInvalidSignupWithoutCreatingGuardian")
  void rejectsInvalidSignupWithoutCreatingGuardian() throws Exception {
    ObjectNode request = request("individual", null, "public");
    long guardians = count("guardian");
    long refreshTokens = count("refresh_token");
    JsonNode error =
        assertProblemResponse(request, 400, "VALIDATION_FAILED", "validation-failed", "입력값 검증 실패");
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
        assertProblemResponse(request, 400, "MALFORMED_REQUEST", "malformed-request", "잘못된 요청 형식");
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
            request("individual", "female", "public"),
            409,
            "EMAIL_ALREADY_EXISTS",
            "email-already-exists",
            "이미 가입된 이메일");
    assertThat(error.has("fieldErrors")).isFalse();
    assertCountUnchanged(guardians, refreshTokens);
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
        post(PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(request)));
  }

  // 비정상 request 필요
  private JsonNode assertProblemResponse(
      ObjectNode request, int status, String code, String type, String title) throws Exception {
    MvcResult result = signup(request).andExpect(status().is(status)).andReturn();
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
    assertThat(error.path("instance").asText()).isEqualTo(PATH);
    String requestId = result.getResponse().getHeader(REQUEST_ID);
    assertThat(requestId).isNotBlank();
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

package io.github.jo0yo0n.mypetmate.guardian;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.jo0yo0n.mypetmate.support.PostgreSqlIntegrationTestSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class GuardianControllerIntegrationTest extends PostgreSqlIntegrationTestSupport {
  private static final String PATH = "/api/guardians/me";
  private static final String REQUEST_ID = "My-Petmate-Request-Id";
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;

  @ParameterizedTest
  @CsvSource({
    "individual,female,active",
    "individual,male,active",
    "couple,,active",
    "family,,active"
  })
  @DisplayName("[M1-API-ME-01] returnsCurrentGuardianForEachProfileType")
  void returnsCurrentGuardianForEachProfileType(String type, String gender, String accountStatus)
      throws Exception {

    JsonNode auth = signup("  Guardian@Example.COM  ", type, gender);
    UUID id = id(auth);
    jdbc.update("UPDATE guardian SET status = ? WHERE id = ?", accountStatus, id);
    MvcResult result =
        mvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(auth)))
            .andExpect(status().isOk())
            .andReturn();
    assertGuardian(result, id, "guardian@example.com", type, gender, "public", accountStatus);
  }

  @ParameterizedTest
  @CsvSource({"individual,female,private", "couple,,private", "family,,public"})
  @DisplayName("[M1-API-ME-03] updatesCurrentGuardianWithValidProfile")
  void updatesCurrentGuardianWithValidProfile(String type, String gender, String visibility)
      throws Exception {

    JsonNode auth = signup("guardian@example.com", "individual", "female");
    MvcResult result =
        update(auth, profile(type, gender, visibility)).andExpect(status().isOk()).andReturn();
    assertGuardian(result, id(auth), "guardian@example.com", type, "female", visibility, "active");
    Map<String, Object> saved = snapshot(id(auth));
    assertThat(saved.get("profile_type")).isEqualTo(type);
    assertThat(saved.get("gender")).isEqualTo("female");
    assertThat(saved.get("identity_visibility")).isEqualTo(visibility);
    assertThat(saved.get("email")).isEqualTo("guardian@example.com");
    assertThat(saved.get("status")).isEqualTo("active");

    if (!type.equals("individual")) {
      MvcResult restored =
          update(auth, profile("individual", "female", "public"))
              .andExpect(status().isOk())
              .andReturn();
      assertGuardian(
          restored, id(auth), "guardian@example.com", "individual", "female", "public", "active");
      Map<String, Object> restoredState = snapshot(id(auth));
      assertThat(restoredState.get("profile_type")).isEqualTo("individual");
      assertThat(restoredState.get("gender")).isEqualTo("female");
      assertThat(restoredState.get("identity_visibility")).isEqualTo("public");
    }
  }

  @Test
  @DisplayName("[M1-API-ME-04A] preservesGuardianWhenUpdateValidationFails")
  void preservesGuardianWhenUpdateValidationFails() throws Exception {

    JsonNode auth = signup("guardian@example.com", "individual", "female");
    Map<String, Object> before = snapshot(id(auth));
    for (ObjectNode request :
        List.of(
            profile("individual", null, "public"),
            profile(null, null, "public"),
            profile("couple", null, null))) {
      JsonNode error =
          assertProblem(
              update(auth, request), "VALIDATION_FAILED", "validation-failed", "입력값 검증 실패");
      assertThat(error.path("fieldErrors").isArray()).isTrue();
      assertThat(error.path("fieldErrors")).isNotEmpty();
      for (JsonNode field : error.path("fieldErrors")) {
        assertThat(field.path("field").asText()).isNotBlank();
        assertThat(field.path("reason").asText()).isNotBlank();
      }
      assertThat(snapshot(id(auth))).isEqualTo(before);
    }
  }

  @Test
  @DisplayName("[M1-API-ME-04B] preservesGuardianWhenUpdateDeserializationFails")
  void preservesGuardianWhenUpdateDeserializationFails() throws Exception {

    JsonNode auth = signup("guardian@example.com", "individual", "female");
    Map<String, Object> before = snapshot(id(auth));
    List<ObjectNode> requests = new ArrayList<>();
    requests.add(profile("unknown", null, "public"));
    requests.add(profile("individual", "unknown", "public"));
    requests.add(profile("couple", null, "unknown"));
    for (String field : List.of("id", "email", "status")) {
      requests.add(profile("couple", null, "private").put(field, "unexpected"));
    }
    for (ObjectNode request : requests) {
      JsonNode error =
          assertProblem(
              update(auth, request), "MALFORMED_REQUEST", "malformed-request", "잘못된 요청 형식");
      assertThat(error.has("fieldErrors")).isFalse();
      assertThat(snapshot(id(auth))).isEqualTo(before);
    }
  }

  @Test
  @DisplayName("[M1-API-ME-05] updatesOnlyAuthenticatedGuardian")
  void updatesOnlyAuthenticatedGuardian() throws Exception {

    JsonNode first = signup("first@example.com", "individual", "female");
    JsonNode other = signup("other@example.com", "individual", "male");
    Map<String, Object> before = snapshot(id(other));
    MvcResult result =
        update(first, profile("couple", null, "private")).andExpect(status().isOk()).andReturn();
    assertGuardian(result, id(first), "first@example.com", "couple", "female", "private", "active");
    assertThat(snapshot(id(first)).get("profile_type")).isEqualTo("couple");
    assertThat(snapshot(id(other))).isEqualTo(before);
  }

  @Test
  @DisplayName("[M1-API-ME-06] preservesFirstGenderForConcurrentUpdates")
  void preservesFirstGenderForConcurrentUpdates() throws Exception {

    JsonNode auth = signup("guardian@example.com", "couple", null);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      try {
        var female = executor.submit(() -> concurrentUpdate(auth, "female", ready, start));
        var male = executor.submit(() -> concurrentUpdate(auth, "male", ready, start));

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        MvcResult femaleResult = female.get(10, TimeUnit.SECONDS);
        MvcResult maleResult = male.get(10, TimeUnit.SECONDS);

        assertThat(
                List.of(
                    femaleResult.getResponse().getStatus(), maleResult.getResponse().getStatus()))
            .containsExactlyInAnyOrder(200, 409);
        String savedGender = femaleResult.getResponse().getStatus() == 200 ? "female" : "male";
        MvcResult success =
            femaleResult.getResponse().getStatus() == 200 ? femaleResult : maleResult;
        MvcResult conflict =
            femaleResult.getResponse().getStatus() == 409 ? femaleResult : maleResult;
        assertGuardian(
            success,
            id(auth),
            "guardian@example.com",
            "individual",
            savedGender,
            "private",
            "active");
        assertProblem(conflict, 409, "STATE_CONFLICT", "state-conflict", "상태 충돌");
        assertThat(assertRequestId(success)).isNotEqualTo(assertRequestId(conflict));
        Map<String, Object> saved = snapshot(id(auth));
        assertThat(saved.get("gender")).isEqualTo(savedGender);
        assertThat(saved.get("profile_type")).isEqualTo("individual");
        assertThat(saved.get("identity_visibility")).isEqualTo("private");
      } finally {
        start.countDown();
      }
    }
  }

  private MvcResult concurrentUpdate(
      JsonNode auth, String gender, CountDownLatch ready, CountDownLatch start) throws Exception {

    ready.countDown();
    if (!start.await(5, TimeUnit.SECONDS)) {
      throw new AssertionError("Timed out waiting to start concurrent profile updates");
    }
    return update(auth, profile("individual", gender, "private")).andReturn();
  }

  @ParameterizedTest
  @CsvSource({"couple,female", "family,male"})
  @DisplayName("[M1-API-ME-07] initializesGenderForFirstIndividualProfile")
  void initializesGenderForFirstIndividualProfile(String initialType, String gender)
      throws Exception {

    JsonNode auth = signup("guardian@example.com", initialType, null);
    assertThat(snapshot(id(auth)).get("gender")).isNull();
    MvcResult result =
        update(auth, profile("individual", gender, "private"))
            .andExpect(status().isOk())
            .andReturn();
    assertGuardian(
        result, id(auth), "guardian@example.com", "individual", gender, "private", "active");
    Map<String, Object> saved = snapshot(id(auth));
    assertThat(saved.get("gender")).isEqualTo(gender);
    assertThat(saved.get("profile_type")).isEqualTo("individual");
    assertThat(saved.get("identity_visibility")).isEqualTo("private");
  }

  @ParameterizedTest
  @CsvSource({"individual,female,male", "couple,female,male", "family,male,female"})
  @DisplayName("[M1-API-ME-08] rejectsChangingPreviouslyStoredGender")
  void rejectsChangingPreviouslyStoredGender(
      String initialType, String storedGender, String requestedGender) throws Exception {

    JsonNode auth = signup("guardian@example.com", "individual", storedGender);
    jdbc.update("UPDATE guardian SET profile_type = ? WHERE id = ?", initialType, id(auth));
    Map<String, Object> before = snapshot(id(auth));
    MvcResult result =
        update(auth, profile("individual", requestedGender, "private"))
            .andExpect(status().isConflict())
            .andReturn();
    assertProblem(result, 409, "STATE_CONFLICT", "state-conflict", "상태 충돌");
    assertThat(snapshot(id(auth))).isEqualTo(before);
  }

  @ParameterizedTest
  @CsvSource({
    "individual,female,couple,male",
    "individual,male,family,female",
    "family,,couple,female",
    "couple,,family,male"
  })
  @DisplayName("[M1-API-ME-09] ignoresGenderForNonIndividualUpdate")
  void ignoresGenderForNonIndividualUpdate(
      String initialType, String storedGender, String requestedType, String requestedGender)
      throws Exception {
    JsonNode auth = signup("guardian@example.com", initialType, storedGender);
    Map<String, Object> before = snapshot(id(auth));
    MvcResult result =
        update(auth, profile(requestedType, requestedGender, "private"))
            .andExpect(status().isOk())
            .andReturn();
    assertGuardian(
        result, id(auth), "guardian@example.com", requestedType, storedGender, "private", "active");
    Map<String, Object> saved = snapshot(id(auth));
    assertThat(saved.get("gender")).isEqualTo(storedGender);
    assertThat(saved.get("profile_type")).isEqualTo(requestedType);
    assertThat(saved.get("identity_visibility")).isEqualTo("private");
    assertThat(saved.get("email")).isEqualTo(before.get("email"));
    assertThat(saved.get("status")).isEqualTo(before.get("status"));
  }

  private JsonNode signup(String email, String type, String gender) throws Exception {
    ObjectNode request =
        profile(type, gender, "public").put("email", email).put("password", "StrongPass123!");
    MvcResult result =
        mvc.perform(
                post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(request)))
            .andExpect(status().isCreated())
            .andReturn();
    return mapper.readTree(result.getResponse().getContentAsString());
  }

  private ObjectNode profile(String type, String gender, String visibility) {
    ObjectNode request =
        mapper.createObjectNode().put("profileType", type).put("identityVisibility", visibility);
    if (gender != null) request.put("gender", gender);
    return request;
  }

  private ResultActions update(JsonNode auth, ObjectNode request) throws Exception {
    return mvc.perform(
        patch(PATH)
            .header(HttpHeaders.AUTHORIZATION, bearer(auth))
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(request)));
  }

  private String bearer(JsonNode auth) {
    return "Bearer " + auth.path("accessToken").asText();
  }

  private UUID id(JsonNode auth) {
    return UUID.fromString(auth.path("guardian").path("id").asText());
  }

  private Map<String, Object> snapshot(UUID id) {
    return jdbc.queryForMap("SELECT * FROM guardian WHERE id = ?", id);
  }

  private void assertGuardian(
      MvcResult result,
      UUID id,
      String email,
      String type,
      String gender,
      String visibility,
      String accountStatus)
      throws Exception {
    content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON).match(result);
    assertRequestId(result);
    JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
    List<String> expected =
        List.of("id", "email", "profileType", "gender", "identityVisibility", "status");
    List<String> actual = new ArrayList<>();
    body.fieldNames().forEachRemaining(actual::add);
    assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    assertThat(body.path("id").asText()).isEqualTo(id.toString());
    assertThat(body.path("email").asText()).isEqualTo(email);
    assertThat(body.path("profileType").asText()).isEqualTo(type);
    assertThat(body.path("identityVisibility").asText()).isEqualTo(visibility);
    assertThat(body.path("status").asText()).isEqualTo(accountStatus);
    if (gender == null) {
      assertThat(body.path("gender").isNull()).isTrue();
    } else {
      assertThat(body.path("gender").asText()).isEqualTo(gender);
    }
  }

  private JsonNode assertProblem(ResultActions actions, String code, String type, String title)
      throws Exception {
    MvcResult result =
        actions
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andReturn();
    return assertProblem(result, 400, code, type, title);
  }

  private JsonNode assertProblem(
      MvcResult result, int expectedStatus, String code, String type, String title)
      throws Exception {
    status().is(expectedStatus).match(result);
    content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON).match(result);
    JsonNode error = mapper.readTree(result.getResponse().getContentAsString());
    assertThat(error.path("status").asInt()).isEqualTo(expectedStatus);
    assertThat(error.path("code").asText()).isEqualTo(code);
    assertThat(error.path("type").asText()).isEqualTo("urn:my-petmate:problem:" + type);
    assertThat(error.path("title").asText()).isEqualTo(title);
    assertThat(error.path("detail").asText()).isNotBlank();
    assertThat(error.path("instance").asText()).isEqualTo(PATH);
    assertThat(error.path("requestId").asText()).isEqualTo(assertRequestId(result));
    if (!code.equals("VALIDATION_FAILED")) assertThat(error.has("fieldErrors")).isFalse();
    return error;
  }

  private String assertRequestId(MvcResult result) {
    String requestId = result.getResponse().getHeader(REQUEST_ID);
    assertThat(requestId).matches("^req-[0-7][0-9A-HJKMNP-TV-Z]{25}$");
    return requestId;
  }
}

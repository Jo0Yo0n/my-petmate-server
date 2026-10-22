package io.github.jo0yo0n.mypetmate.guardian;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jo0yo0n.mypetmate.support.PostgreSqlIntegrationTestSupport;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

abstract class GuardianWithdrawalTestSupport extends PostgreSqlIntegrationTestSupport {
  static final String PATH = "/api/guardians/me/withdraw";
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;

  JsonNode signup() throws Exception {
    MvcResult result =
        mvc.perform(
                post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
            {"email":"guardian@example.com","password":"StrongPass123!",
             "profileType":"individual","gender":"female","discoveryStatus":"searching"}
            """))
            .andExpect(status().isCreated())
            .andReturn();
    return mapper.readTree(result.getResponse().getContentAsString());
  }

  UUID id(JsonNode auth) {
    return UUID.fromString(auth.path("guardian").path("id").asText());
  }

  MvcResult withdraw(JsonNode auth) throws Exception {
    return mvc.perform(
            post(PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + auth.path("accessToken").asText()))
        .andReturn();
  }

  MvcResult refresh(JsonNode auth) throws Exception {
    return mvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    mapper.writeValueAsBytes(
                        mapper
                            .createObjectNode()
                            .put("refreshToken", auth.path("refreshToken").asText()))))
        .andReturn();
  }

  Map<String, Object> guardian(UUID id) {
    return jdbc.queryForMap("SELECT * FROM guardian WHERE id = ?", id);
  }

  List<Map<String, Object>> tokens(UUID id) {
    return jdbc.queryForList("SELECT * FROM refresh_token WHERE guardian_id = ? ORDER BY id", id);
  }

  void assertProblem(MvcResult result, int expectedStatus, String code) throws Exception {
    assertThat(result.getResponse().getStatus()).isEqualTo(expectedStatus);
    assertThat(
            MediaType.parseMediaType(result.getResponse().getContentType())
                .isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .isTrue();
    JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
    assertThat(body.path("status").asInt()).isEqualTo(expectedStatus);
    assertThat(body.path("code").asText()).isEqualTo(code);
    assertThat(body.path("instance").asText()).isEqualTo(result.getRequest().getRequestURI());
    String requestId = result.getResponse().getHeader("My-Petmate-Request-Id");
    assertThat(requestId).matches("^req-[0-7][0-9A-HJKMNP-TV-Z]{25}$");
    assertThat(body.path("requestId").asText()).isEqualTo(requestId);
    assertThat(body.path("detail").asText()).isNotBlank();
    assertThat(body.has("fieldErrors")).isFalse();
  }
}

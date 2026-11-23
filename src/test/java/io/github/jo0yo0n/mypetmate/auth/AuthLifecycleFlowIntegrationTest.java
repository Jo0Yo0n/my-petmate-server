package io.github.jo0yo0n.mypetmate.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.jo0yo0n.mypetmate.support.PostgreSqlIntegrationTestSupport;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** 가입부터 로그아웃까지 한 계정의 토큰 수명주기를 하나의 흐름으로 확인한다. */
@Sql(PostgreSqlIntegrationTestSupport.ANSAN_REGIONS_SQL)
@SpringBootTest
@AutoConfigureMockMvc
class AuthLifecycleFlowIntegrationTest extends PostgreSqlIntegrationTestSupport {
  private static final String ME_PATH = "/api/guardians/me";

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired RefreshTokenGenerator refreshTokenGenerator;

  @Test
  @DisplayName("[M1-API-LC-01] followsTokenLifecycleFromSignupToLogout")
  void followsTokenLifecycleFromSignupToLogout() throws Exception {
    JsonNode auth = signup();
    String beforeAccessToken = auth.path("accessToken").asText();
    String beforeRefreshToken = auth.path("refreshToken").asText();
    assertThat(beforeAccessToken).isNotBlank();
    assertThat(beforeRefreshToken).isNotBlank();

    assertThat(body(me(beforeAccessToken).andExpect(status().isOk())))
        .isEqualTo(auth.path("guardian"));

    JsonNode afterPatch = body(patchMe(beforeAccessToken, "새닉네임").andExpect(status().isOk()));
    ObjectNode expected = auth.path("guardian").deepCopy();
    expected.put("nickname", "새닉네임");
    assertThat(afterPatch).isEqualTo(expected);

    JsonNode afterRefresh = body(refresh(beforeRefreshToken).andExpect(status().isOk()));
    String afterAccessToken = afterRefresh.path("accessToken").asText();
    String afterRefreshToken = afterRefresh.path("refreshToken").asText();
    assertThat(afterAccessToken).isNotBlank();
    assertThat(afterRefreshToken).isNotBlank();

    patchMe(afterAccessToken, "새새닉네임").andExpect(status().isOk());
    me(afterAccessToken)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nickname").value("새새닉네임"));

    refresh(beforeRefreshToken)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_REFRESH_INVALID"));

    // 현 동작은 폐기된 refresh token의 재사용 뒤에도 새 토큰은 유효함.
    // 재사용 감지 시 토큰 계열 전체를 폐기하도록 정책이 바뀌면 이 assertion과 아래 양성 대조가 깨질 수 있음.
    me(afterAccessToken)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nickname").value("새새닉네임"));

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT revoked_at FROM refresh_token WHERE token_hash = ?",
                OffsetDateTime.class,
                refreshTokenGenerator.hash(afterRefreshToken)))
        .isNull();

    logout(afterRefreshToken).andExpect(status().isNoContent()).andExpect(content().string(""));
    logout(afterRefreshToken).andExpect(status().isNoContent()).andExpect(content().string(""));

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT revoked_at FROM refresh_token WHERE token_hash = ?",
                OffsetDateTime.class,
                refreshTokenGenerator.hash(afterRefreshToken)))
        .isNotNull();

    refresh(afterRefreshToken)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_REFRESH_INVALID"));
  }

  /** 가입과 PATCH가 함께 쓰는 프로필 본문. */
  private ObjectNode profile(String nickname) {
    ObjectNode request =
        mapper
            .createObjectNode()
            .put("nickname", nickname)
            .put("profileType", "individual")
            .put("gender", "female")
            .put("discoveryStatus", "searching");
    request.putArray("activityRegionIds").add(IL_DONG_ID.toString());
    request
        .putArray("availableTimeSlots")
        .addObject()
        .put("dayOfWeek", "mon")
        .put("startTime", "09:00")
        .put("endTime", "10:30");
    return request;
  }

  private JsonNode signup() throws Exception {
    ObjectNode request =
        profile("보호자").put("email", "guardian@example.com").put("password", "StrongPass123!");
    return body(
        mvc.perform(
                post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(request)))
            .andExpect(status().isCreated()));
  }

  private ResultActions me(String accessToken) throws Exception {
    return mvc.perform(get(ME_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
  }

  private ResultActions patchMe(String accessToken, String nickname) throws Exception {
    return mvc.perform(
        patch(ME_PATH)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(profile(nickname))));
  }

  private ResultActions refresh(String refreshToken) throws Exception {
    return mvc.perform(
        post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                mapper.writeValueAsBytes(
                    mapper.createObjectNode().put("refreshToken", refreshToken))));
  }

  private ResultActions logout(String refreshToken) throws Exception {
    return mvc.perform(
        post("/api/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                mapper.writeValueAsBytes(
                    mapper.createObjectNode().put("refreshToken", refreshToken))));
  }

  private JsonNode body(ResultActions actions) throws Exception {
    return mapper.readTree(actions.andReturn().getResponse().getContentAsString());
  }
}

package io.github.jo0yo0n.mypetmate.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.jo0yo0n.mypetmate.support.PostgreSqlIntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(PostgreSqlIntegrationTestSupport.ANSAN_REGIONS_SQL)
class SignupActivityIntegrationTest extends PostgreSqlIntegrationTestSupport {

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper mapper;
  @Autowired private JdbcTemplate jdbc;

  @ParameterizedTest
  @ValueSource(strings = {"searching", "paused"})
  void storesAndReturnsSuppliedActivity(String discoveryStatus) throws Exception {
    var result =
        signup(request(discoveryStatus, true))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.guardian.discoveryStatus").value(discoveryStatus))
            .andExpect(jsonPath("$.guardian.nickname").value("보호자"))
            .andExpect(jsonPath("$.guardian.photoUrl").value(nullValue()))
            .andExpect(jsonPath("$.guardian.activityRegions.length()").value(1))
            .andExpect(jsonPath("$.guardian.activityRegions[0].id").value(IL_DONG_ID.toString()))
            .andExpect(jsonPath("$.guardian.activityRegions[0].path.length()").value(3))
            .andExpect(jsonPath("$.guardian.activityRegions[0].path[0].level").value("province"))
            .andExpect(jsonPath("$.guardian.activityRegions[0].path[0].name").value("경기도"))
            .andExpect(jsonPath("$.guardian.activityRegions[0].path[1].level").value("district"))
            .andExpect(jsonPath("$.guardian.activityRegions[0].path[1].name").value("안산시 상록구"))
            .andExpect(jsonPath("$.guardian.activityRegions[0].path[2].level").value("locality"))
            .andExpect(jsonPath("$.guardian.activityRegions[0].path[2].name").value("일동"))
            .andExpect(jsonPath("$.guardian.availableTimeSlots.length()").value(1))
            .andExpect(jsonPath("$.guardian.availableTimeSlots[0].dayOfWeek").value("mon"))
            .andExpect(jsonPath("$.guardian.availableTimeSlots[0].startTime").value("23:50"))
            .andExpect(jsonPath("$.guardian.availableTimeSlots[0].endTime").value("24:00"))
            .andReturn();

    UUID guardianId =
        UUID.fromString(
            mapper.readTree(result.getResponse().getContentAsString()).at("/guardian/id").asText());
    assertThat(
            jdbc.queryForList(
                "select region_id from guardian_activity_region where guardian_id = ?",
                UUID.class,
                guardianId))
        .containsExactly(IL_DONG_ID);
    assertThat(
            jdbc.query(
                "select day_of_week, start_minute, end_minute from guardian_available_time_slot where guardian_id = ?",
                (row, index) -> new StoredSlot(row.getString(1), row.getInt(2), row.getInt(3)),
                guardianId))
        .containsExactly(new StoredSlot("mon", 1430, 1440));
  }

  @ParameterizedTest
  @CsvSource({
    "activityRegionIds,omitted",
    "activityRegionIds,empty",
    "activityRegionIds,null",
    "activityRegionIds,duplicate",
    "activityRegionIds[1],unknown",
    "availableTimeSlots,omitted",
    "availableTimeSlots,empty",
    "availableTimeSlots,null"
  })
  void searchingRejectsInvalidActivityLists(String field, String input) throws Exception {
    ObjectNode request = request("searching", true);
    switch (input) {
      case "unknown" -> request.withArray("activityRegionIds").add(UUID.randomUUID().toString());
      case "duplicate" -> request.withArray(field).add(IL_DONG_ID.toString());
      case "omitted" -> request.remove(field);
      case "empty" -> request.putArray(field);
      case "null" -> request.putNull(field);
      default -> throw new IllegalArgumentException(input);
    }

    signup(request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors.length()").value(1))
        .andExpect(jsonPath("$.fieldErrors[0].field").value(field));
    assertThat(jdbc.queryForObject("select count(*) from guardian", Long.class)).isZero();
    assertNoStoredActivity();
  }

  @ParameterizedTest
  @ValueSource(strings = {"omitted", "empty", "null"})
  void pausedAllowsOmittedEmptyOrNullActivityLists(String input) throws Exception {
    ObjectNode request = request("paused", false);
    switch (input) {
      case "omitted" -> {}
      case "empty" -> {
        request.putArray("activityRegionIds");
        request.putArray("availableTimeSlots");
      }
      case "null" -> {
        request.putNull("activityRegionIds");
        request.putNull("availableTimeSlots");
      }
      default -> throw new IllegalArgumentException(input);
    }

    signup(request)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.guardian.discoveryStatus").value("paused"))
        .andExpect(jsonPath("$.guardian.activityRegions").isArray())
        .andExpect(jsonPath("$.guardian.activityRegions").isEmpty())
        .andExpect(jsonPath("$.guardian.availableTimeSlots").isArray())
        .andExpect(jsonPath("$.guardian.availableTimeSlots").isEmpty());
    assertNoStoredActivity();
  }

  private void assertNoStoredActivity() {
    assertThat(jdbc.queryForObject("select count(*) from guardian_activity_region", Long.class))
        .isZero();
    assertThat(jdbc.queryForObject("select count(*) from guardian_available_time_slot", Long.class))
        .isZero();
  }

  private ObjectNode request(String discoveryStatus, boolean includeActivity) {
    ObjectNode request =
        mapper
            .createObjectNode()
            .put("nickname", "보호자")
            .put("email", "signup-activity@example.com")
            .put("password", "StrongPass123!")
            .put("profileType", "family")
            .put("discoveryStatus", discoveryStatus);
    if (includeActivity) {
      request.putArray("activityRegionIds").add(IL_DONG_ID.toString());
      request
          .putArray("availableTimeSlots")
          .addObject()
          .put("dayOfWeek", "mon")
          .put("startTime", "23:50")
          .put("endTime", "24:00");
    }
    return request;
  }

  private ResultActions signup(ObjectNode request) throws Exception {
    return mvc.perform(
        post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(request)));
  }

  private record StoredSlot(String dayOfWeek, int startMinute, int endMinute) {}
}

package io.github.jo0yo0n.mypetmate.guardian;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
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
import java.time.LocalTime;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@Sql(PostgreSqlIntegrationTestSupport.ANSAN_REGIONS_SQL)
@SpringBootTest
@AutoConfigureMockMvc
class GuardianControllerIntegrationTest extends PostgreSqlIntegrationTestSupport {
  private static final String PATH = "/api/guardians/me";
  private static final String I_DONG_ID = "c39f2f04-fcde-404c-a459-f4845cbf888e";
  private static final String GOJAN_DONG_ID = "137455a6-38b2-44af-ae72-cedbcffb1629";
  private static final String REQUEST_ID = "My-Petmate-Request-Id";
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;

  // GET /api/guardians/me
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
    assertGuardian(result, id, "guardian@example.com", type, gender, "searching", accountStatus);
  }

  // PATCH /api/guardians/me
  @ParameterizedTest
  @CsvSource({"individual,female,paused", "couple,,paused", "family,,searching"})
  @DisplayName("[M1-API-ME-03] patchUpdatesCurrentGuardianWithValidProfile")
  void patchUpdatesCurrentGuardianWithValidProfile(
      String type, String gender, String discoveryStatus) throws Exception {

    JsonNode auth = signup("guardian@example.com", "individual", "female");
    MvcResult result =
        update(auth, profile(type, gender, discoveryStatus)).andExpect(status().isOk()).andReturn();
    assertGuardian(
        result, id(auth), "guardian@example.com", type, "female", discoveryStatus, "active");
    Map<String, Object> saved = snapshot(id(auth));
    assertThat(saved.get("profile_type")).isEqualTo(type);
    assertThat(saved.get("gender")).isEqualTo("female");
    assertThat(saved.get("discovery_status")).isEqualTo(discoveryStatus);
    assertThat(saved.get("email")).isEqualTo("guardian@example.com");
    assertThat(saved.get("status")).isEqualTo("active");

    if (!type.equals("individual")) {
      MvcResult restored =
          update(auth, profile("individual", "female", "searching"))
              .andExpect(status().isOk())
              .andReturn();
      assertGuardian(
          restored,
          id(auth),
          "guardian@example.com",
          "individual",
          "female",
          "searching",
          "active");
      Map<String, Object> restoredState = snapshot(id(auth));
      assertThat(restoredState.get("profile_type")).isEqualTo("individual");
      assertThat(restoredState.get("gender")).isEqualTo("female");
      assertThat(restoredState.get("discovery_status")).isEqualTo("searching");
    }
  }

  @Test
  void patchReplacesProvidedActivityListsAndReturnsTheSavedProfile() throws Exception {
    JsonNode auth = signup("guardian@example.com", "individual", "female");
    ObjectNode request = profile("couple", null, "paused").put("nickname", "보리네");
    request.putArray("activityRegionIds").add(I_DONG_ID);
    request.putArray("availableTimeSlots");

    update(auth, request)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nickname").value("보리네"))
        .andExpect(jsonPath("$.activityRegions.length()").value(1))
        .andExpect(jsonPath("$.activityRegions[0].id").value(I_DONG_ID))
        .andExpect(jsonPath("$.availableTimeSlots[0].dayOfWeek").value("mon"));

    request.putNull("activityRegionIds");
    request
        .putArray("availableTimeSlots")
        .addObject()
        .put("dayOfWeek", "sat")
        .put("startTime", "14:00")
        .put("endTime", "15:00");

    for (int attempt = 0; attempt < 2; attempt++) {
      update(auth, request)
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.activityRegions[0].id").value(I_DONG_ID))
          .andExpect(jsonPath("$.availableTimeSlots.length()").value(1))
          .andExpect(jsonPath("$.availableTimeSlots[0].dayOfWeek").value("sat"));
    }

    mvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(auth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nickname").value("보리네"))
        .andExpect(jsonPath("$.activityRegions.length()").value(1))
        .andExpect(jsonPath("$.activityRegions[0].id").value(I_DONG_ID))
        .andExpect(jsonPath("$.availableTimeSlots.length()").value(1))
        .andExpect(jsonPath("$.availableTimeSlots[0].startTime").value("14:00"));
  }

  @Test
  void patchStoresThreeDistinctLocalityIds() throws Exception {
    JsonNode auth = signup("guardian@example.com", "individual", "female");
    ObjectNode request = profile("individual", "female", "searching");
    request.withArray("activityRegionIds").add(I_DONG_ID).add(GOJAN_DONG_ID);
    var expectedIds = containsInAnyOrder(IL_DONG_ID.toString(), I_DONG_ID, GOJAN_DONG_ID);

    update(auth, request)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.activityRegions[*].id", expectedIds));
    mvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(auth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.activityRegions[*].id", expectedIds));
  }

  @Test
  void patchRollsBackProfileAndBothActivityListsWhenCommitFails() throws Exception {
    JsonNode auth = signup("guardian@example.com", "individual", "female");
    UUID guardianId = id(auth);
    var beforeProfile = snapshot(guardianId);
    var beforeRegions =
        jdbc.queryForList(
            "SELECT * FROM guardian_activity_region WHERE guardian_id = ? ORDER BY id", guardianId);
    var beforeSlots =
        jdbc.queryForList(
            "SELECT * FROM guardian_available_time_slot WHERE guardian_id = ? ORDER BY id",
            guardianId);

    ObjectNode request = profile("family", null, "searching").put("nickname", "보리네");
    request.putArray("activityRegionIds").add(I_DONG_ID);
    request
        .putArray("availableTimeSlots")
        .addObject()
        .put("dayOfWeek", "sat")
        .put("startTime", "14:00")
        .put("endTime", "15:00");

    jdbc.execute(
        """
        CREATE FUNCTION fail_activity_commit() RETURNS trigger LANGUAGE plpgsql AS $$
        BEGIN
          RAISE EXCEPTION 'test activity commit failure' USING ERRCODE = '23514';
        END;
        $$
        """);
    try {
      jdbc.execute(
          """
          CREATE CONSTRAINT TRIGGER fail_activity_commit
          AFTER INSERT ON guardian_available_time_slot
          DEFERRABLE INITIALLY DEFERRED
          FOR EACH ROW WHEN (NEW.day_of_week = 'sat')
          EXECUTE FUNCTION fail_activity_commit()
          """);

      var result = update(auth, request).andExpect(status().isInternalServerError()).andReturn();
      assertThat(result.getResolvedException())
          .hasStackTraceContaining("test activity commit failure");

      assertThat(snapshot(guardianId)).isEqualTo(beforeProfile);
      assertThat(
              jdbc.queryForList(
                  "SELECT * FROM guardian_activity_region WHERE guardian_id = ? ORDER BY id",
                  guardianId))
          .isEqualTo(beforeRegions);
      assertThat(
              jdbc.queryForList(
                  "SELECT * FROM guardian_available_time_slot WHERE guardian_id = ? ORDER BY id",
                  guardianId))
          .isEqualTo(beforeSlots);
    } finally {
      jdbc.execute("DROP TRIGGER IF EXISTS fail_activity_commit ON guardian_available_time_slot");
      jdbc.execute("DROP FUNCTION fail_activity_commit()");
    }
  }

  @Test
  void patchReturnsEachInvalidRegionIndexAndRollsBackProfileUpdate() throws Exception {
    JsonNode auth = signup("guardian@example.com", "individual", "female");
    Map<String, Object> before = snapshot(id(auth));
    ObjectNode request = profile("family", null, "searching").put("nickname", "보리네");
    request
        .putArray("activityRegionIds")
        .add("00000000-0000-0000-0000-000000000001")
        .add(IL_DONG_ID.toString())
        .add("2d032bc3-37d8-4ab3-b634-c1cd5d472eb4");

    JsonNode error =
        assertProblem(update(auth, request), "VALIDATION_FAILED", "validation-failed", "입력값 검증 실패");
    JsonNode fieldErrors = error.path("fieldErrors");
    assertThat(fieldErrors.isArray()).isTrue();
    assertThat(fieldErrors)
        .extracting(item -> item.path("field").asText())
        .containsExactly("activityRegionIds[0]", "activityRegionIds[2]");
    assertThat(fieldErrors)
        .allSatisfy(
            item -> assertThat(item.path("reason").asText()).isEqualTo("존재하지 않는 읍·면·동 입니다."));
    assertThat(snapshot(id(auth))).isEqualTo(before);
  }

  @Test
  void patchRejectsAnAlreadyUsedNicknameAsStateConflict() throws Exception {
    JsonNode auth = signup("first@example.com", "individual", "female");
    signup("other@example.com", "individual", "male");
    Map<String, Object> before = snapshot(id(auth));

    update(auth, profile("couple", null, "paused").put("nickname", "other"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("STATE_CONFLICT"));
    assertThat(snapshot(id(auth))).isEqualTo(before);
  }

  @Test
  void patchRejectsUnsupportedAcceptWithoutSavingProfile() throws Exception {
    JsonNode auth = signup("guardian@example.com", "individual", "female");
    Map<String, Object> before = snapshot(id(auth));
    ObjectNode request = profile("family", null, "paused").put("nickname", "보리네");

    mvc.perform(patchRequest(auth, request).accept(MediaType.APPLICATION_XML))
        .andExpect(status().isNotAcceptable())
        .andExpect(jsonPath("$.code").value("NOT_ACCEPTABLE"));
    assertThat(snapshot(id(auth))).isEqualTo(before);
  }

  @ParameterizedTest
  @CsvSource({
    "activityRegionIds,omitted",
    "activityRegionIds,null",
    "activityRegionIds,empty",
    "availableTimeSlots,omitted",
    "availableTimeSlots,null",
    "availableTimeSlots,empty"
  })
  void patchSearchingRequiresEachActivityListEvenWhenAlreadySaved(String field, String input)
      throws Exception {
    JsonNode auth = signup("guardian@example.com", "individual", "female");
    ObjectNode request = profile("family", null, "searching").put("nickname", "보리네");
    switch (input) {
      case "omitted" -> request.remove(field);
      case "null" -> request.putNull(field);
      case "empty" -> request.putArray(field);
      default -> throw new IllegalArgumentException(input);
    }

    assertRejectedProfileField(auth, request, field);
  }

  @ParameterizedTest
  @CsvSource({"duplicate,2", "tooMany,4"})
  void patchRejectsDuplicateOrTooManyActivityRegionIds(String input, int count) throws Exception {
    JsonNode auth = signup("guardian@example.com", "individual", "female");
    ObjectNode request = profile("family", null, "paused").put("nickname", "보리네");
    var ids = request.putArray("activityRegionIds");
    for (int index = 0; index < count; index++) {
      UUID id = "duplicate".equals(input) ? IL_DONG_ID : UUID.randomUUID();
      ids.add(id.toString());
    }

    assertRejectedProfileField(auth, request, "activityRegionIds");
  }

  @ParameterizedTest
  @CsvSource({"09:00,10:30", "10:00,11:00"})
  void patchRejectsDuplicateOrOverlappingTimeSlots(String start, String end) throws Exception {
    JsonNode auth = signup("guardian@example.com", "individual", "female");
    ObjectNode request = profile("family", null, "searching").put("nickname", "보리네");
    request
        .withArray("availableTimeSlots")
        .addObject()
        .put("dayOfWeek", "mon")
        .put("startTime", start)
        .put("endTime", end);

    assertRejectedProfileField(auth, request, "availableTimeSlots");
  }

  @ParameterizedTest
  @CsvSource({
    "omitted,09:00,10:30,dayOfWeek",
    "null,09:00,10:30,dayOfWeek",
    "mon,9:00,10:30,startTime",
    "mon,09:05,10:30,startTime",
    "mon,09:00,10:35,endTime",
    "mon,10:30,09:00,endTime",
    "mon,09:00,09:00,endTime",
    "mon,23:50,00:00,endTime",
    "mon,23:50,24:10,endTime",
    "mon,24:00,24:00,startTime"
  })
  void patchRejectsInvalidTimeSlot(String day, String start, String end, String field)
      throws Exception {
    JsonNode auth = signup("guardian@example.com", "individual", "female");
    ObjectNode request = profile("individual", "female", "searching").put("nickname", "guardian");
    var slot =
        request
            .putArray("availableTimeSlots")
            .addObject()
            .put("startTime", start)
            .put("endTime", end);
    switch (day) {
      case "omitted" -> slot.remove("dayOfWeek");
      case "null" -> slot.putNull("dayOfWeek");
      default -> slot.put("dayOfWeek", day);
    }

    assertRejectedProfileField(auth, request, "availableTimeSlots[0]." + field);
  }

  @Test
  void patchStoresEndOfDayAndRejectsOverlappingTimeSlot() throws Exception {
    JsonNode auth = signup("guardian@example.com", "individual", "female");
    ObjectNode request = profile("individual", "female", "searching").put("nickname", "guardian");
    var slots = request.putArray("availableTimeSlots");
    slots.addObject().put("dayOfWeek", "mon").put("startTime", "23:50").put("endTime", "24:00");
    slots.addObject().put("dayOfWeek", "mon").put("startTime", "23:40").put("endTime", "23:50");
    slots.addObject().put("dayOfWeek", "tue").put("startTime", "00:00").put("endTime", "00:10");

    update(auth, request)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.availableTimeSlots[*].endTime", hasItem("24:00")));
    mvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(auth)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$.availableTimeSlots[*].endTime", containsInAnyOrder("24:00", "23:50", "00:10")));

    ((ObjectNode) slots.get(1)).put("endTime", "24:00");
    assertRejectedProfileField(auth, request, "availableTimeSlots");
  }

  @Test
  void patchAcceptsTwentyOneTimeSlotsStartingAtMidnightAndRejectsTwentyTwo() throws Exception {
    JsonNode auth = signup("guardian@example.com", "individual", "female");
    ObjectNode request = profile("individual", "female", "searching").put("nickname", "guardian");
    var slots = request.putArray("availableTimeSlots");
    for (int index = 0; index < 22; index++) {
      LocalTime start = LocalTime.MIDNIGHT.plusMinutes(index * 10L);
      slots
          .addObject()
          .put("dayOfWeek", "mon")
          .put("startTime", start.toString())
          .put("endTime", start.plusMinutes(10).toString());
    }

    assertRejectedProfileField(auth, request, "availableTimeSlots");
    slots.remove(21);
    update(auth, request)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.availableTimeSlots.length()").value(21));
    mvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(auth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.availableTimeSlots.length()").value(21))
        .andExpect(jsonPath("$.availableTimeSlots[*].startTime", hasItem("00:00")));
  }

  @ParameterizedTest
  @CsvSource({"가1,가1", "Ab가12345678901234567,Ab가12345678901234567"})
  void patchAcceptsNicknameAtLengthBoundaries(String nickname, String expected) throws Exception {
    JsonNode auth = signup("guardian@example.com", "individual", "female");

    update(auth, profile("individual", "female", "paused").put("nickname", nickname))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nickname").value(expected));
  }

  @ParameterizedTest
  @CsvSource({"가", "Ab가123456789012345678", "가!", "가 나", "ㄱ나"})
  void patchRejectsNicknameOutsideLengthOrCharacterRules(String nickname) throws Exception {
    JsonNode auth = signup("guardian@example.com", "individual", "female");

    assertRejectedProfileField(
        auth, profile("individual", "female", "paused").put("nickname", nickname), "nickname");
  }

  @Test
  void patchRejectsNicknameContainingWhitespace() throws Exception {
    JsonNode auth = signup("guardian@example.com", "individual", "female");
    for (String nickname : List.of(" 보호자", "보호자 ", "보 호자", "\t보호자", "보호자\n", "보호\u3000자")) {
      assertRejectedProfileField(
          auth, profile("individual", "female", "paused").put("nickname", nickname), "nickname");
    }
  }

  @Test
  void patchRejectsMissingNickname() throws Exception {
    JsonNode auth = signup("guardian@example.com", "individual", "female");

    assertRejectedProfileField(auth, profile("family", null, "paused"), "nickname");
  }

  @Test
  @DisplayName("[M1-API-ME-04A] patchPreservesGuardianWhenUpdateValidationFails")
  void patchPreservesGuardianWhenUpdateValidationFails() throws Exception {

    JsonNode auth = signup("guardian@example.com", "individual", "female");
    Map<String, Object> before = snapshot(id(auth));
    for (ObjectNode request :
        List.of(
            profile("individual", null, "searching"),
            profile(null, null, "searching"),
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
  @DisplayName("[M1-API-ME-04B] patchPreservesGuardianWhenUpdateDeserializationFails")
  void patchPreservesGuardianWhenUpdateDeserializationFails() throws Exception {

    JsonNode auth = signup("guardian@example.com", "individual", "female");
    Map<String, Object> before = snapshot(id(auth));
    List<ObjectNode> requests = new ArrayList<>();
    requests.add(profile("unknown", null, "searching"));
    requests.add(profile("individual", "unknown", "searching"));
    requests.add(profile("couple", null, "unknown"));
    requests.add(profile("couple", null, "paused").put("nickname", 1234));
    requests.add(profile("couple", null, "paused").put("nickname", true));
    for (String field : List.of("id", "email", "status")) {
      requests.add(profile("couple", null, "paused").put(field, "unexpected"));
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
  @DisplayName("[M1-API-ME-05] patchUpdatesOnlyAuthenticatedGuardian")
  void patchUpdatesOnlyAuthenticatedGuardian() throws Exception {

    JsonNode first = signup("first@example.com", "individual", "female");
    JsonNode other = signup("other@example.com", "individual", "male");
    Map<String, Object> before = snapshot(id(other));
    MvcResult result =
        update(first, profile("couple", null, "paused")).andExpect(status().isOk()).andReturn();
    assertGuardian(result, id(first), "first@example.com", "couple", "female", "paused", "active");
    assertThat(snapshot(id(first)).get("profile_type")).isEqualTo("couple");
    assertThat(snapshot(id(other))).isEqualTo(before);
  }

  @Test
  @DisplayName("[M1-API-ME-06] patchPreservesFirstGenderForConcurrentUpdates")
  void patchPreservesFirstGenderForConcurrentUpdates() throws Exception {

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
            "paused",
            "active");
        assertProblem(conflict, 409, "STATE_CONFLICT", "state-conflict", "상태 충돌");
        assertThat(assertRequestId(success)).isNotEqualTo(assertRequestId(conflict));
        Map<String, Object> saved = snapshot(id(auth));
        assertThat(saved.get("gender")).isEqualTo(savedGender);
        assertThat(saved.get("profile_type")).isEqualTo("individual");
        assertThat(saved.get("discovery_status")).isEqualTo("paused");
      } finally {
        start.countDown();
      }
    }
  }

  @ParameterizedTest
  @CsvSource({"couple,female", "family,male"})
  @DisplayName("[M1-API-ME-07] patchInitializesGenderForFirstIndividualProfile")
  void patchInitializesGenderForFirstIndividualProfile(String initialType, String gender)
      throws Exception {

    JsonNode auth = signup("guardian@example.com", initialType, null);
    assertThat(snapshot(id(auth)).get("gender")).isNull();
    MvcResult result =
        update(auth, profile("individual", gender, "paused"))
            .andExpect(status().isOk())
            .andReturn();
    assertGuardian(
        result, id(auth), "guardian@example.com", "individual", gender, "paused", "active");
    Map<String, Object> saved = snapshot(id(auth));
    assertThat(saved.get("gender")).isEqualTo(gender);
    assertThat(saved.get("profile_type")).isEqualTo("individual");
    assertThat(saved.get("discovery_status")).isEqualTo("paused");
  }

  @ParameterizedTest
  @CsvSource({"individual,female,male", "couple,female,male", "family,male,female"})
  @DisplayName("[M1-API-ME-08] patchRejectsChangingPreviouslyStoredGender")
  void patchRejectsChangingPreviouslyStoredGender(
      String initialType, String storedGender, String requestedGender) throws Exception {

    JsonNode auth = signup("guardian@example.com", "individual", storedGender);
    jdbc.update("UPDATE guardian SET profile_type = ? WHERE id = ?", initialType, id(auth));
    Map<String, Object> before = snapshot(id(auth));
    MvcResult result =
        update(auth, profile("individual", requestedGender, "paused"))
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
  @DisplayName("[M1-API-ME-09] patchIgnoresGenderForNonIndividualUpdate")
  void patchIgnoresGenderForNonIndividualUpdate(
      String initialType, String storedGender, String requestedType, String requestedGender)
      throws Exception {
    JsonNode auth = signup("guardian@example.com", initialType, storedGender);
    Map<String, Object> before = snapshot(id(auth));
    MvcResult result =
        update(auth, profile(requestedType, requestedGender, "paused"))
            .andExpect(status().isOk())
            .andReturn();
    assertGuardian(
        result, id(auth), "guardian@example.com", requestedType, storedGender, "paused", "active");
    Map<String, Object> saved = snapshot(id(auth));
    assertThat(saved.get("gender")).isEqualTo(storedGender);
    assertThat(saved.get("profile_type")).isEqualTo(requestedType);
    assertThat(saved.get("discovery_status")).isEqualTo("paused");
    assertThat(saved.get("email")).isEqualTo(before.get("email"));
    assertThat(saved.get("status")).isEqualTo(before.get("status"));
  }

  // POST /api/guardians/me/withdraw
  @Test
  @ExtendWith(OutputCaptureExtension.class)
  @DisplayName("[M1-API-WD-01] withdrawsCurrentGuardianAndRevokesTokens")
  void withdrawsCurrentGuardianAndRevokesTokens(CapturedOutput output) throws Exception {
    JsonNode auth = signup("guardian@example.com", "individual", "female");
    JsonNode other = signup("other@example.com", "individual", "male");
    login("guardian@example.com");
    Map<String, Object> before = snapshot(id(auth));
    Map<String, Object> otherBefore = snapshot(id(other));
    var otherTokens = tokens(id(other));
    var beforeTokens = tokens(id(auth));
    assertThat(beforeTokens).hasSize(2);

    MvcResult withdrawalResult = withdraw(auth).andReturn();
    assertWithdrawalSuccess(withdrawalResult);
    String requestId = withdrawalResult.getResponse().getHeader(REQUEST_ID);
    String withdrawalLog =
        output
            .getAll()
            .lines()
            .filter(line -> line.contains("event=GUARDIAN_WITHDRAWN"))
            .findFirst()
            .orElseThrow();
    assertThat(withdrawalLog).contains("guardianId=" + id(auth), "requestId=" + requestId);

    Map<String, Object> after = snapshot(id(auth));
    assertThat(after.get("status")).isEqualTo("withdrawn");
    for (String field :
        List.of(
            "id",
            "email",
            "password_hash",
            "profile_type",
            "gender",
            "discovery_status",
            "created_at")) {
      assertThat(after.get(field)).isEqualTo(before.get(field));
    }
    var afterTokens = tokens(id(auth));
    assertThat(afterTokens).hasSize(beforeTokens.size());
    for (int index = 0; index < afterTokens.size(); index++) {
      assertThat(afterTokens.get(index).get("revoked_at")).isNotNull();
      for (String field : List.of("id", "guardian_id", "token_hash", "created_at", "expires_at")) {
        assertThat(afterTokens.get(index).get(field)).isEqualTo(beforeTokens.get(index).get(field));
      }
    }
    assertThat(snapshot(id(other))).isEqualTo(otherBefore);
    assertThat(tokens(id(other))).isEqualTo(otherTokens);
  }

  @Test
  @DisplayName("[M1-API-WD-06] rejectsRepeatedWithdrawalWithAccountWithdrawn")
  void rejectsRepeatedWithdrawalWithAccountWithdrawn() throws Exception {
    JsonNode auth = signup("guardian@example.com", "individual", "female");
    MvcResult first = withdraw(auth).andReturn();
    assertWithdrawalSuccess(first);

    Map<String, Object> before = snapshot(id(auth));
    var beforeTokens = tokens(id(auth));
    MvcResult repeated = withdraw(auth).andReturn();

    assertProblem(repeated, 403, "ACCOUNT_WITHDRAWN", "account-withdrawn", "탈퇴한 계정");
    assertThat(assertRequestId(repeated)).isNotEqualTo(assertRequestId(first));
    assertThat(snapshot(id(auth))).isEqualTo(before);
    assertThat(tokens(id(auth))).isEqualTo(beforeTokens);
  }

  @ParameterizedTest
  @CsvSource({
    "false,401,AUTH_TOKEN_INVALID,auth-token-invalid,유효하지 않은 access token",
    "true,403,ACCOUNT_RESTRICTED,account-restricted,정지된 계정"
  })
  @DisplayName("[M1-API-WD-07] rejectsUnauthorizedWithdrawal")
  void rejectsUnauthorizedWithdrawal(
      boolean restricted, int status, String code, String type, String title) throws Exception {
    JsonNode auth = signup("guardian@example.com", "individual", "female");
    if (restricted) {
      jdbc.update("UPDATE guardian SET status = 'temporarily_restricted' WHERE id = ?", id(auth));
    }
    Map<String, Object> before = snapshot(id(auth));
    var beforeTokens = tokens(id(auth));
    var request = post(PATH + "/withdraw");
    if (restricted) request.header(HttpHeaders.AUTHORIZATION, bearer(auth));
    assertProblem(mvc.perform(request).andReturn(), status, code, type, title);
    assertThat(snapshot(id(auth))).isEqualTo(before);
    assertThat(tokens(id(auth))).isEqualTo(beforeTokens);
  }

  // Test helpers
  private void assertRejectedProfileField(JsonNode auth, ObjectNode request, String field)
      throws Exception {
    Map<String, Object> before = snapshot(id(auth));
    mvc.perform(patchRequest(auth, request))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors.length()").value(1))
        .andExpect(jsonPath("$.fieldErrors[0].field").value(field));
    assertThat(snapshot(id(auth))).isEqualTo(before);
  }

  private MvcResult concurrentUpdate(
      JsonNode auth, String gender, CountDownLatch ready, CountDownLatch start) throws Exception {

    ready.countDown();
    if (!start.await(5, TimeUnit.SECONDS)) {
      throw new AssertionError("Timed out waiting to start concurrent profile updates");
    }
    return update(auth, profile("individual", gender, "paused")).andReturn();
  }

  ResultActions withdraw(JsonNode auth) throws Exception {
    return mvc.perform(post(PATH + "/withdraw").header(HttpHeaders.AUTHORIZATION, bearer(auth)));
  }

  void assertWithdrawalSuccess(MvcResult result) throws Exception {
    status().isNoContent().match(result);
    content().string("").match(result);
    assertThat(result.getResponse().getContentType()).isNull();
    assertRequestId(result);
  }

  JsonNode login(String email) throws Exception {
    MvcResult result =
        mvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        mapper.writeValueAsBytes(
                            mapper
                                .createObjectNode()
                                .put("email", email)
                                .put("password", "StrongPass123!"))))
            .andExpect(status().isOk())
            .andReturn();
    return mapper.readTree(result.getResponse().getContentAsString());
  }

  List<Map<String, Object>> tokens(UUID guardianId) {
    return jdbc.queryForList(
        "SELECT * FROM refresh_token WHERE guardian_id = ? ORDER BY id", guardianId);
  }

  private JsonNode signup(String email, String type, String gender) throws Exception {
    ObjectNode request =
        profile(type, gender, "searching").put("email", email).put("password", "StrongPass123!");
    request.put("nickname", email.strip().split("@", 2)[0]);
    MvcResult result =
        mvc.perform(
                post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(request)))
            .andExpect(status().isCreated())
            .andReturn();
    return mapper.readTree(result.getResponse().getContentAsString());
  }

  private ObjectNode profile(String type, String gender, String discoveryStatus) {
    ObjectNode request =
        mapper.createObjectNode().put("profileType", type).put("discoveryStatus", discoveryStatus);
    if (gender != null) request.put("gender", gender);
    if ("searching".equals(discoveryStatus)) {
      request.putArray("activityRegionIds").add(IL_DONG_ID.toString());
      request
          .putArray("availableTimeSlots")
          .addObject()
          .put("dayOfWeek", "mon")
          .put("startTime", "09:00")
          .put("endTime", "10:30");
    }
    return request;
  }

  private ResultActions update(JsonNode auth, ObjectNode request) throws Exception {
    if (!request.has("nickname")) {
      request.put("nickname", auth.path("guardian").path("nickname").asText());
    }
    return mvc.perform(patchRequest(auth, request));
  }

  private MockHttpServletRequestBuilder patchRequest(JsonNode auth, ObjectNode request)
      throws Exception {
    return patch(PATH)
        .header(HttpHeaders.AUTHORIZATION, bearer(auth))
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(request));
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
      String discoveryStatus,
      String accountStatus)
      throws Exception {
    content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON).match(result);
    assertRequestId(result);
    JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
    List<String> expected =
        List.of(
            "id",
            "email",
            "profileType",
            "gender",
            "discoveryStatus",
            "status",
            "nickname",
            "photoUrl",
            "activityRegions",
            "availableTimeSlots");
    List<String> actual = new ArrayList<>();
    body.fieldNames().forEachRemaining(actual::add);
    assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    assertThat(body.path("id").asText()).isEqualTo(id.toString());
    assertThat(body.path("email").asText()).isEqualTo(email);
    assertThat(body.path("profileType").asText()).isEqualTo(type);
    assertThat(body.path("discoveryStatus").asText()).isEqualTo(discoveryStatus);
    assertThat(body.path("status").asText()).isEqualTo(accountStatus);
    assertThat(body.path("nickname").asText())
        .isEqualToIgnoringCase(email.substring(0, email.indexOf("@")));
    assertThat(body.path("photoUrl").isNull()).isTrue();
    assertThat(body.path("activityRegions").isArray()).isTrue();
    assertThat(body.path("availableTimeSlots").isArray()).isTrue();
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
    assertThat(error.path("instance").asText()).isEqualTo(result.getRequest().getRequestURI());
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

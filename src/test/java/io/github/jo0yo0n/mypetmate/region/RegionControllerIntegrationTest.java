package io.github.jo0yo0n.mypetmate.region;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jo0yo0n.mypetmate.support.PostgreSqlIntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class RegionControllerIntegrationTest extends PostgreSqlIntegrationTestSupport {
  private static final String GYEONGGI = "d5c56a23-2c5e-5405-84b3-1b8c7c658c75";
  private static final String SANGNOK = "c328470e-f1ce-5c02-9349-5cb331c61fc6";
  private static final String DANWON = "862f29b3-3ed6-5c33-983f-6683ce04fb8b";
  private static final String SEOUL = "af55e452-0ebc-5fb3-a43b-6e8b86b5d6b6";
  private static final String JONGNO = "10000000-0000-0000-0000-000000000001";
  private static final String OTHER_DONG = "10000000-0000-0000-0000-000000000002";
  private static final String SAME_NAME_FIRST = "10000000-0000-0000-0000-000000000003";
  private static final String SAME_NAME_SECOND = "10000000-0000-0000-0000-000000000004";

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper mapper;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void prepareRegions() {
    jdbc.execute("TRUNCATE TABLE region CASCADE");
    new ResourceDatabasePopulator(new FileSystemResource("scripts/fixtures/ansan-regions.sql"))
        .execute(jdbc.getDataSource());
    insert(SEOUL, "서울특별시", "sido", null);
    insert(JONGNO, "종로구", "sgg", SEOUL);
    insert(OTHER_DONG, "일동", "umd", JONGNO);
  }

  @Test
  void listsOnlyProvincesWithoutAuthentication() throws Exception {
    var result =
        mvc.perform(get("/api/regions"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[0].id").value(GYEONGGI))
            .andExpect(jsonPath("$.items[0].name").value("경기도"))
            .andExpect(jsonPath("$.items[1].id").value(SEOUL))
            .andExpect(jsonPath("$.items[1].name").value("서울특별시"))
            .andReturn();
    var root = mapper.readTree(result.getResponse().getContentAsByteArray());
    assertThat(root.size()).isEqualTo(1);
    for (var item : root.get("items")) {
      assertThat(item.size()).isEqualTo(4);
      assertThat(item.get("level").asText()).isEqualTo("sido");
      assertThat(item.has("parentId")).isTrue();
      assertThat(item.get("parentId").isNull()).isTrue();
      UUID.fromString(item.get("id").asText());
    }
  }

  @Test
  void listsOnlySeoulSi() throws Exception {
    mvc.perform(get("/api/regions").param("parentId", SEOUL))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].id").value(JONGNO))
        .andExpect(jsonPath("$.items[0].name").value("종로구"))
        .andExpect(jsonPath("$.items[0].level").value("sgg"))
        .andExpect(jsonPath("$.items[0].parentId").value(SEOUL));
  }

  @Test
  void listsOnlyGyeonggiDoInNameOrder() throws Exception {
    mvc.perform(get("/api/regions").param("parentId", GYEONGGI))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.items[0].id").value(DANWON))
        .andExpect(jsonPath("$.items[0].name").value("안산시 단원구"))
        .andExpect(jsonPath("$.items[1].id").value(SANGNOK))
        .andExpect(jsonPath("$.items[1].name").value("안산시 상록구"))
        .andExpect(jsonPath("$.items[0].level").value("sgg"))
        .andExpect(jsonPath("$.items[1].parentId").value(GYEONGGI));
  }

  @Test
  void listsEupAndMyeonAsUmdChildren() throws Exception {
    String gapyeongGun = "2220a40b-c3fc-5954-a305-fb80a27c7ed2";
    String gapyeongEup = "9e64d9e0-a5a0-54ba-89ad-47e38c6379a1";
    String seorakMyeon = "dada850f-8c2c-57bf-a6dc-bb3b99d74a8f";
    insert(gapyeongGun, "가평군", "sgg", GYEONGGI);
    insert(gapyeongEup, "가평읍", "umd", gapyeongGun);
    insert(seorakMyeon, "설악면", "umd", gapyeongGun);

    mvc.perform(get("/api/regions").param("parentId", gapyeongGun))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.items[0].id").value(gapyeongEup))
        .andExpect(jsonPath("$.items[0].name").value("가평읍"))
        .andExpect(jsonPath("$.items[0].level").value("umd"))
        .andExpect(jsonPath("$.items[0].parentId").value(gapyeongGun))
        .andExpect(jsonPath("$.items[1].id").value(seorakMyeon))
        .andExpect(jsonPath("$.items[1].name").value("설악면"))
        .andExpect(jsonPath("$.items[1].level").value("umd"))
        .andExpect(jsonPath("$.items[1].parentId").value(gapyeongGun));
  }

  @Test
  void doesNotMixSameNamedUmdFromOtherSgg() throws Exception {
    var result =
        mvc.perform(get("/api/regions").param("parentId", SANGNOK))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[1].id").value(IL_DONG_ID.toString()))
            .andExpect(jsonPath("$.items[1].name").value("일동"))
            .andReturn();
    for (var item : mapper.readTree(result.getResponse().getContentAsByteArray()).get("items")) {
      assertThat(item.get("parentId").asText()).isEqualTo(SANGNOK);
      assertThat(item.get("level").asText()).isEqualTo("umd");
      assertThat(item.get("id").asText()).isNotEqualTo(OTHER_DONG);
    }
    mvc.perform(get("/api/regions").param("parentId", JONGNO))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].id").value(OTHER_DONG))
        .andExpect(jsonPath("$.items[0].name").value("일동"))
        .andExpect(jsonPath("$.items[0].parentId").value(JONGNO));
  }

  @Test
  void ordersEqualNamesByIdRegardlessOfInsertionOrder() throws Exception {
    insert(SAME_NAME_SECOND, "가동", "umd", JONGNO);
    insert(SAME_NAME_FIRST, "가동", "umd", JONGNO);
    mvc.perform(get("/api/regions").param("parentId", JONGNO))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(SAME_NAME_FIRST))
        .andExpect(jsonPath("$.items[1].id").value(SAME_NAME_SECOND))
        .andExpect(jsonPath("$.items[2].id").value(OTHER_DONG));
  }

  @Test
  void returnsEmptyArrayForSido() throws Exception {
    mvc.perform(get("/api/regions").param("parentId", IL_DONG_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"items\":[]}"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "not-a-uuid", "1-1-1-1-1"})
  void rejectsMalformedParentId(String parentId) throws Exception {
    problem(mvc.perform(get("/api/regions").param("parentId", parentId)), 400, "VALIDATION_FAILED")
        .andExpect(jsonPath("$.fieldErrors[0].field").value("parentId"));
  }

  @Test
  void returnsNotFoundForUnknownParent() throws Exception {
    problem(
        mvc.perform(get("/api/regions").param("parentId", UUID.randomUUID().toString())),
        404,
        "RESOURCE_NOT_FOUND");
  }

  @Test
  void rejectsUnauthenticatedPostToRegions() throws Exception {
    mvc.perform(post("/api/regions")).andExpect(status().isUnauthorized());
  }

  private ResultActions problem(ResultActions result, int statusCode, String code)
      throws Exception {
    var response =
        result
            .andExpect(status().is(statusCode))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(statusCode))
            .andExpect(jsonPath("$.code").value(code))
            .andExpect(jsonPath("$.instance").value("/api/regions"))
            .andExpect(jsonPath("$.type").isNotEmpty())
            .andExpect(jsonPath("$.title").isNotEmpty())
            .andExpect(jsonPath("$.detail").isNotEmpty())
            .andReturn()
            .getResponse();
    var requestId = response.getHeader("My-Petmate-Request-Id");
    assertThat(requestId).matches("^req-[0-7][0-9A-HJKMNP-TV-Z]{25}$");
    assertThat(mapper.readTree(response.getContentAsByteArray()).get("requestId").asText())
        .isEqualTo(requestId);
    return result;
  }

  private void insert(String id, String name, String level, String parentId) {
    jdbc.update(
        "INSERT INTO region (id, name, level, parent_id) VALUES (?, ?, ?, ?)",
        UUID.fromString(id),
        name,
        level,
        parentId == null ? null : UUID.fromString(parentId));
  }
}

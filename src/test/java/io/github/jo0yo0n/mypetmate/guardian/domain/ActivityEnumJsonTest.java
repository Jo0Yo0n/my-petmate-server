package io.github.jo0yo0n.mypetmate.guardian.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jo0yo0n.mypetmate.region.domain.RegionLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ActivityEnumJsonTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @ParameterizedTest
  @CsvSource({
    "MONDAY,mon",
    "TUESDAY,tue",
    "WEDNESDAY,wed",
    "THURSDAY,thu",
    "FRIDAY,fri",
    "SATURDAY,sat",
    "SUNDAY,sun"
  })
  void convertsWeekdayToAndFromContractJson(Weekday weekday, String value) throws Exception {
    String json = "\"" + value + "\"";
    assertThat(objectMapper.writeValueAsString(weekday)).isEqualTo(json);
    assertThat(objectMapper.readValue(json, Weekday.class)).isEqualTo(weekday);
  }

  @ParameterizedTest
  @CsvSource({"SIDO,sido", "SGG,sgg", "UMD,umd"})
  void convertsRegionLevelToAndFromContractJson(RegionLevel level, String value) throws Exception {
    String json = "\"" + value + "\"";
    assertThat(objectMapper.writeValueAsString(level)).isEqualTo(json);
    assertThat(objectMapper.readValue(json, RegionLevel.class)).isEqualTo(level);
  }

  @Test
  void rejectsUnsupportedValuesAndPreservesNull() throws Exception {
    for (String json : new String[] {"\"MONDAY\"", "\"unknown\"", "\"\""}) {
      assertThatThrownBy(() -> objectMapper.readValue(json, Weekday.class))
          .isInstanceOf(JsonProcessingException.class);
    }
    for (String json : new String[] {"\"SIDO\"", "\"unknown\"", "\"\""}) {
      assertThatThrownBy(() -> objectMapper.readValue(json, RegionLevel.class))
          .isInstanceOf(JsonProcessingException.class);
    }
    assertThat(objectMapper.readValue("null", Weekday.class)).isNull();
    assertThat(objectMapper.readValue("null", RegionLevel.class)).isNull();
  }
}

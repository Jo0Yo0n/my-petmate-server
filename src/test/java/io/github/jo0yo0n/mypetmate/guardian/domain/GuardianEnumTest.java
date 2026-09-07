package io.github.jo0yo0n.mypetmate.guardian.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jo0yo0n.mypetmate.guardian.persistence.GenderConverter;
import io.github.jo0yo0n.mypetmate.guardian.persistence.GuardianStatusConverter;
import io.github.jo0yo0n.mypetmate.guardian.persistence.IdentityVisibilityConverter;
import io.github.jo0yo0n.mypetmate.guardian.persistence.ProfileTypeConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GuardianEnumTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @DisplayName("[M1-DTO-01] serializesEnumsAsContractValues")
  @Test
  void serializesEnumsAsContractValues() throws Exception {
    assertThat(objectMapper.writeValueAsString(ProfileType.INDIVIDUAL)).isEqualTo("\"individual\"");
    assertThat(objectMapper.writeValueAsString(Gender.FEMALE)).isEqualTo("\"female\"");
    assertThat(objectMapper.writeValueAsString(IdentityVisibility.PRIVATE))
        .isEqualTo("\"private\"");
    assertThat(objectMapper.writeValueAsString(GuardianStatus.TEMPORARILY_RESTRICTED))
        .isEqualTo("\"temporarily_restricted\"");
  }

  @DisplayName("[M1-DTO-01] deserializesExactContractValues")
  @Test
  void deserializesExactContractValues() throws Exception {
    assertThat(objectMapper.readValue("\"couple\"", ProfileType.class))
        .isEqualTo(ProfileType.COUPLE);
    assertThat(objectMapper.readValue("\"male\"", Gender.class)).isEqualTo(Gender.MALE);
    assertThat(objectMapper.readValue("\"public\"", IdentityVisibility.class))
        .isEqualTo(IdentityVisibility.PUBLIC);
    assertThat(objectMapper.readValue("\"withdrawn\"", GuardianStatus.class))
        .isEqualTo(GuardianStatus.WITHDRAWN);
  }

  @DisplayName("[M1-DTO-01] rejectsCaseVariantsAndUnknownValues")
  @Test
  void rejectsCaseVariantsAndUnknownValues() {
    assertThatThrownBy(() -> objectMapper.readValue("\"INDIVIDUAL\"", ProfileType.class))
        .hasMessageContaining("Unknown profile type value");
    assertThatThrownBy(() -> objectMapper.readValue("\"unknown\"", Gender.class))
        .hasMessageContaining("Unknown gender value");
  }

  @DisplayName("[M1-DTO-01] convertersUseLowercaseDatabaseValues")
  @Test
  void convertersUseLowercaseDatabaseValues() {
    ProfileTypeConverter profileTypeConverter = new ProfileTypeConverter();
    GenderConverter genderConverter = new GenderConverter();
    IdentityVisibilityConverter visibilityConverter = new IdentityVisibilityConverter();
    GuardianStatusConverter statusConverter = new GuardianStatusConverter();

    assertThat(profileTypeConverter.convertToDatabaseColumn(ProfileType.FAMILY))
        .isEqualTo("family");
    assertThat(profileTypeConverter.convertToEntityAttribute("individual"))
        .isEqualTo(ProfileType.INDIVIDUAL);
    assertThat(genderConverter.convertToDatabaseColumn(Gender.MALE)).isEqualTo("male");
    assertThat(visibilityConverter.convertToEntityAttribute("private"))
        .isEqualTo(IdentityVisibility.PRIVATE);
    assertThat(statusConverter.convertToDatabaseColumn(GuardianStatus.ACTIVE)).isEqualTo("active");
  }

  @DisplayName("[M1-DTO-01] convertersPreserveNullsAndRejectUnknownDatabaseValues")
  @Test
  void convertersPreserveNullsAndRejectUnknownDatabaseValues() {
    ProfileTypeConverter converter = new ProfileTypeConverter();

    assertThat(converter.convertToDatabaseColumn(null)).isNull();
    assertThat(converter.convertToEntityAttribute(null)).isNull();
    assertThatThrownBy(() -> converter.convertToEntityAttribute("INDIVIDUAL"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unknown profile type value");
  }
}

package io.github.jo0yo0n.mypetmate.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import io.github.jo0yo0n.mypetmate.guardian.domain.DiscoveryStatus;
import io.github.jo0yo0n.mypetmate.guardian.domain.Gender;
import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

@JsonTest
public class SignupRequestJsonTest {

  @Autowired private ObjectMapper objectMapper;

  @DisplayName("[보강] acceptsValidSignupJson")
  @Test
  void acceptsValidSignupJson() throws Exception {
    String json =
        """
            {"email":"guardian@example.com", "password":"StrongPass123!", "profileType":"individual", "gender":"female", "discoveryStatus":"searching"}
            """;

    assertThat(objectMapper.readValue(json, SignupRequest.class))
        .isEqualTo(
            new SignupRequest(
                "guardian@example.com",
                "StrongPass123!",
                ProfileType.INDIVIDUAL,
                Gender.FEMALE,
                DiscoveryStatus.SEARCHING));
  }

  @DisplayName("[보강] acceptsCoupleAndFamilySignupJsonWithoutGender")
  @Test
  void acceptsCoupleAndFamilySignupJsonWithoutGender() throws Exception {
    String coupleJson =
        """
            {"email":"couple@example.com", "password":"StrongPass123!", "profileType":"couple", "discoveryStatus":"paused"}
            """;
    String familyJson =
        """
            {"email":"family@example.com", "password":"StrongPass123!", "profileType":"family", "discoveryStatus":"searching"}
            """;

    assertThat(objectMapper.readValue(coupleJson, SignupRequest.class))
        .isEqualTo(
            new SignupRequest(
                "couple@example.com",
                "StrongPass123!",
                ProfileType.COUPLE,
                null,
                DiscoveryStatus.PAUSED));
    assertThat(objectMapper.readValue(familyJson, SignupRequest.class))
        .isEqualTo(
            new SignupRequest(
                "family@example.com",
                "StrongPass123!",
                ProfileType.FAMILY,
                null,
                DiscoveryStatus.SEARCHING));
  }

  @DisplayName("[M1-DTO-08A] rejectsUnknownSignupJsonField")
  @Test
  void rejectsUnknownSignupJsonField() {
    String json =
        """
            {"email":"guardian@example.com","password":"StrongPass123!","profileType":"individual",\
            "gender":"female","discoveryStatus":"searching","unexpected":true}
            """;

    assertThatThrownBy(() -> objectMapper.readValue(json, SignupRequest.class))
        .isInstanceOf(JsonProcessingException.class);
  }

  @DisplayName("[M1-DTO-08D] rejectsUnknownProfileTypeInSignupJson")
  @Test
  void rejectsUnknownProfileTypeInSignupJson() {
    assertSignupJsonRejected("unknown", "female", "searching", "profileType");
  }

  @DisplayName("[보강] rejectsUnknownGenderInSignupJson")
  @Test
  void rejectsUnknownGenderInSignupJson() {
    assertSignupJsonRejected("individual", "unknown", "searching", "gender");
  }

  @DisplayName("[보강] rejectsUnknownDiscoveryStatusInSignupJson")
  @Test
  void rejectsUnknownDiscoveryStatusInSignupJson() {
    assertSignupJsonRejected("individual", "female", "unknown", "discoveryStatus");
  }

  private void assertSignupJsonRejected(
      String profileType, String gender, String discoveryStatus, String expectedField) {
    String json =
        """
            {"email":"guardian@example.com","password":"StrongPass123!","profileType":"%s",\
            "gender":"%s","discoveryStatus":"%s"}
            """
            .formatted(profileType, gender, discoveryStatus);

    assertThatThrownBy(() -> objectMapper.readValue(json, SignupRequest.class))
        .isInstanceOfSatisfying(
            ValueInstantiationException.class,
            exception ->
                assertThat(exception.getPath())
                    .isNotEmpty()
                    .last()
                    .extracting(Reference::getFieldName)
                    .isEqualTo(expectedField));
  }
}

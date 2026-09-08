package io.github.jo0yo0n.mypetmate.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import io.github.jo0yo0n.mypetmate.guardian.domain.Gender;
import io.github.jo0yo0n.mypetmate.guardian.domain.IdentityVisibility;
import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

@JsonTest
public class SignupRequestJsonTest {

  @Autowired private ObjectMapper objectMapper;

  @DisplayName("[M1-DTO-02] acceptsValidSignupJson")
  @Test
  void acceptsValidSignupJson() throws Exception {
    String json =
        """
            {"email":"guardian@example.com", "password":"StrongPass123!", "profileType":"individual", "gender":"female", "identityVisibility":"public"}
            """;

    assertThat(objectMapper.readValue(json, SignupRequest.class))
        .isEqualTo(
            new SignupRequest(
                "guardian@example.com",
                "StrongPass123!",
                ProfileType.INDIVIDUAL,
                Gender.FEMALE,
                IdentityVisibility.PUBLIC));
  }

  @DisplayName("[M1-DTO-02] acceptsCoupleAndFamilySignupJsonWithoutGender")
  @Test
  void acceptsCoupleAndFamilySignupJsonWithoutGender() throws Exception {
    String coupleJson =
        """
            {"email":"couple@example.com", "password":"StrongPass123!", "profileType":"couple", "identityVisibility":"private"}
            """;
    String familyJson =
        """
            {"email":"family@example.com", "password":"StrongPass123!", "profileType":"family", "identityVisibility":"public"}
            """;

    assertThat(objectMapper.readValue(coupleJson, SignupRequest.class))
        .isEqualTo(
            new SignupRequest(
                "couple@example.com",
                "StrongPass123!",
                ProfileType.COUPLE,
                null,
                IdentityVisibility.PRIVATE));
    assertThat(objectMapper.readValue(familyJson, SignupRequest.class))
        .isEqualTo(
            new SignupRequest(
                "family@example.com",
                "StrongPass123!",
                ProfileType.FAMILY,
                null,
                IdentityVisibility.PUBLIC));
  }

  @DisplayName("[M1-DTO-08] rejectsUnknownSignupJsonField")
  @Test
  void rejectsUnknownSignupJsonField() {
    String json =
        """
            {"email":"guardian@example.com","password":"StrongPass123!","profileType":"individual",\
            "gender":"female","identityVisibility":"public","unexpected":true}
            """;

    assertThatThrownBy(() -> objectMapper.readValue(json, SignupRequest.class))
        .isInstanceOf(JsonProcessingException.class);
  }

  @DisplayName("[M1-DTO-08] rejectsUnknownProfileTypeInSignupJson")
  @Test
  void rejectsUnknownProfileTypeInSignupJson() {
    assertSignupJsonRejected("unknown", "female", "public", "profileType");
  }

  @DisplayName("[M1-DTO-08] rejectsUnknownGenderInSignupJson")
  @Test
  void rejectsUnknownGenderInSignupJson() {
    assertSignupJsonRejected("individual", "unknown", "public", "gender");
  }

  @DisplayName("[M1-DTO-08] rejectsUnknownIdentityVisibilityInSignupJson")
  @Test
  void rejectsUnknownIdentityVisibilityInSignupJson() {
    assertSignupJsonRejected("individual", "female", "unknown", "identityVisibility");
  }

  private void assertSignupJsonRejected(
      String profileType, String gender, String identityVisibility, String expectedField) {
    String json =
        """
            {"email":"guardian@example.com","password":"StrongPass123!","profileType":"%s",\
            "gender":"%s","identityVisibility":"%s"}
            """
            .formatted(profileType, gender, identityVisibility);

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

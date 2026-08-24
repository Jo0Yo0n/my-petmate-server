package io.github.jo0yo0n.mypetmate.guardian.dto;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jo0yo0n.mypetmate.guardian.domain.Gender;
import io.github.jo0yo0n.mypetmate.guardian.domain.IdentityVisibility;
import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RequestValidationTest {

  private static Validator validator;

  @BeforeAll
  static void createValidator() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void acceptsValidIndividualCoupleAndFamilySignupRequests() {
    assertThat(violations(signup(ProfileType.INDIVIDUAL, Gender.FEMALE))).isEmpty();
    assertThat(violations(signup(ProfileType.COUPLE, null))).isEmpty();
    assertThat(violations(signup(ProfileType.FAMILY, null))).isEmpty();
  }

  @Test
  void requiresGenderForIndividualSignup() {
    Set<ConstraintViolation<SignupRequest>> violations =
        violations(signup(ProfileType.INDIVIDUAL, null));

    assertThat(violations)
        .anySatisfy(
            violation -> {
              assertThat(violation.getPropertyPath().toString()).isEqualTo("gender");
              assertThat(violation.getMessage()).isEqualTo("profileType이 individual이면 필수입니다.");
            });
  }

  @Test
  void forbidsGenderForCoupleAndFamilySignup() {
    assertThat(violations(signup(ProfileType.COUPLE, Gender.FEMALE)))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("gender");
    assertThat(violations(signup(ProfileType.FAMILY, Gender.MALE)))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("gender");
  }

  @Test
  void appliesTheSameProfileRuleToGuardianUpdates() {
    GuardianUpdateRequest valid =
        new GuardianUpdateRequest(ProfileType.COUPLE, null, IdentityVisibility.PRIVATE);
    GuardianUpdateRequest invalid =
        new GuardianUpdateRequest(ProfileType.FAMILY, Gender.FEMALE, IdentityVisibility.PUBLIC);

    assertThat(violations(valid)).isEmpty();
    assertThat(violations(invalid))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("gender");
  }

  @Test
  void acceptsPasswordContainingEveryRequiredAsciiCharacterGroup() {
    assertThat(violations(signup("StrongPass123!"))).isEmpty();
    assertThat(violations(signup("Aa1!" + "a".repeat(68)))).isEmpty();
  }

  @Test
  void rejectsPasswordMissingAnyRequiredCharacterGroup() {
    assertPasswordInvalid("strongpass123!");
    assertPasswordInvalid("STRONGPASS123!");
    assertPasswordInvalid("StrongPassword!");
    assertPasswordInvalid("StrongPass123");
  }

  @Test
  void rejectsNonAsciiWhitespaceAndOutOfRangePasswords() {
    assertPasswordInvalid("Strong한글123!");
    assertPasswordInvalid("Strong Pass123!");
    assertPasswordInvalid("StrongPass123?");
    assertPasswordInvalid("StrongPass123`");
    assertPasswordInvalid("Aa1!");
    assertPasswordInvalid("Aa1!" + "a".repeat(69));
  }

  @Test
  void validatesEmailAndRequiredSignupFields() {
    SignupRequest request = new SignupRequest("invalid", null, null, null, null);

    assertThat(violations(request))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("email", "password", "profileType", "identityVisibility");
  }

  @Test
  void loginDoesNotReapplySignupPasswordComplexity() {
    LoginRequest request = new LoginRequest("guardian@example.com", "x");

    assertThat(violations(request)).isEmpty();
  }

  @Test
  void validatesRefreshTokenLength() {
    assertThat(violations(new RefreshRequest("t".repeat(20)))).isEmpty();
    assertThat(violations(new RefreshRequest("short")))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("refreshToken");
  }

  private SignupRequest signup(ProfileType profileType, Gender gender) {
    return new SignupRequest(
        "guardian@example.com", "StrongPass123!", profileType, gender, IdentityVisibility.PUBLIC);
  }

  private SignupRequest signup(String password) {
    return new SignupRequest(
        "guardian@example.com",
        password,
        ProfileType.INDIVIDUAL,
        Gender.FEMALE,
        IdentityVisibility.PUBLIC);
  }

  private void assertPasswordInvalid(String password) {
    assertThat(violations(signup(password)))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("password");
  }

  private <T> Set<ConstraintViolation<T>> violations(T value) {
    return validator.validate(value);
  }
}

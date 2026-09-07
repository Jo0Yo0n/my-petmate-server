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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RequestValidationTest {

  private static Validator validator;

  @BeforeAll
  static void createValidator() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @DisplayName("[M1-DTO-02] acceptsValidIndividualCoupleAndFamilySignupRequests")
  @Test
  void acceptsValidIndividualCoupleAndFamilySignupRequests() {
    assertThat(violations(signup(ProfileType.INDIVIDUAL, Gender.FEMALE))).isEmpty();
    assertThat(violations(signup(ProfileType.COUPLE, null))).isEmpty();
    assertThat(violations(signup(ProfileType.FAMILY, null))).isEmpty();
  }

  @DisplayName("[M1-DTO-02] requiresGenderForIndividualSignup")
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

  @DisplayName("[M1-DTO-02] forbidsGenderForCoupleAndFamilySignup")
  @Test
  void forbidsGenderForCoupleAndFamilySignup() {
    assertThat(violations(signup(ProfileType.COUPLE, Gender.FEMALE)))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("gender");
    assertThat(violations(signup(ProfileType.FAMILY, Gender.MALE)))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("gender");
  }

  @DisplayName("[M1-DTO-02] appliesTheSameProfileRuleToGuardianUpdates")
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

  @DisplayName("[M1-DTO-03] acceptsPasswordContainingEveryRequiredAsciiCharacterGroup")
  @Test
  void acceptsPasswordContainingEveryRequiredAsciiCharacterGroup() {
    assertThat(violations(signup("StrongPass123!"))).isEmpty();
    assertThat(violations(signup("Aa1!" + "a".repeat(68)))).isEmpty();
  }

  @DisplayName("[M1-DTO-03] rejectsPasswordMissingAnyRequiredCharacterGroup")
  @Test
  void rejectsPasswordMissingAnyRequiredCharacterGroup() {
    assertPasswordInvalid("strongpass123!");
    assertPasswordInvalid("STRONGPASS123!");
    assertPasswordInvalid("StrongPassword!");
    assertPasswordInvalid("StrongPass123");
  }

  @DisplayName("[M1-DTO-03] rejectsNonAsciiWhitespaceAndOutOfRangePasswords")
  @Test
  void rejectsNonAsciiWhitespaceAndOutOfRangePasswords() {
    assertPasswordInvalid("Strong한글123!");
    assertPasswordInvalid("Strong Pass123!");
    assertPasswordInvalid("StrongPass123?");
    assertPasswordInvalid("StrongPass123`");
    assertPasswordInvalid("Aa1!");
    assertPasswordInvalid("Aa1!" + "a".repeat(69));
  }

  @DisplayName("[M1-DTO-02, M1-DTO-03, M1-DTO-05] validatesEmailAndRequiredSignupFields")
  @Test
  void validatesEmailAndRequiredSignupFields() {
    SignupRequest request = new SignupRequest("invalid", null, null, null, null);

    assertThat(violations(request))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("email", "password", "profileType", "identityVisibility");
  }

  @DisplayName("[M1-DTO-04] normalizesSignupAndLoginEmailsBeforeValidation")
  @Test
  void normalizesSignupAndLoginEmailsBeforeValidation() {
    SignupRequest signup =
        new SignupRequest(
            "  GUARDIAN@EXAMPLE.COM  ",
            "StrongPass123!",
            ProfileType.INDIVIDUAL,
            Gender.FEMALE,
            IdentityVisibility.PUBLIC);
    LoginRequest login = new LoginRequest("  GUARDIAN@EXAMPLE.COM  ", "x");

    assertThat(signup.email()).isEqualTo("guardian@example.com");
    assertThat(login.email()).isEqualTo("guardian@example.com");
    assertThat(violations(signup)).isEmpty();
    assertThat(violations(login)).isEmpty();
  }

  @DisplayName("[M1-DTO-05] validatesNormalizedEmailBlankAndLengthBoundaries")
  @Test
  void validatesNormalizedEmailBlankAndLengthBoundaries() {
    String longestValidEmail = longestValidEmail();
    String tooLongEmail = longestValidEmail + "x";

    assertThat(violations(signupWithEmail("  ")))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("email");
    assertThat(violations(signupWithEmail(" " + longestValidEmail + " "))).isEmpty();
    assertThat(violations(signupWithEmail(tooLongEmail)))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("email");
  }

  @DisplayName("[M1-DTO-05] rejectsNonAsciiSignupAndLoginEmails")
  @Test
  void rejectsNonAsciiSignupAndLoginEmails() {
    assertViolation(
        signupWithEmail("guardian@예시.한국"), "email", "영문, 숫자, 기호로 구성된 이메일 주소만 사용할 수 있습니다.");
    assertViolation(
        new LoginRequest("müller@example.com", "StringPass123!"),
        "email",
        "영문, 숫자, 기호로 구성된 이메일 주소만 사용할 수 있습니다.");
  }

  @DisplayName("[M1-DTO-05] rejectsSignupEmailShorterThanThreeCharacters")
  @Test
  void rejectsSignupEmailShorterThanThreeCharacters() {
    assertViolation(signupWithEmail("a@"), "email", "3자 이상 254자 이하이어야 합니다.");
  }

  @DisplayName("[M1-DTO-03] rejectsBlankSignupPassword")
  @Test
  void rejectsBlankSignupPassword() {
    assertViolation(signup(""), "password", "8자 이상 72자 이하이어야 합니다.");
  }

  @DisplayName("[M1-DTO-06] loginDoesNotReapplySignupPasswordComplexity")
  @Test
  void loginDoesNotReapplySignupPasswordComplexity() {
    LoginRequest request = new LoginRequest("guardian@example.com", "x");

    assertThat(violations(request)).isEmpty();
  }

  @DisplayName("[M1-DTO-07] acceptsValidRefreshToken")
  @Test
  void acceptsValidRefreshToken() {
    assertThat(violations(new RefreshRequest("A".repeat(43)))).isEmpty();
  }

  @DisplayName("[M1-DTO-07] rejectsRefreshTokenShorterThan43Characters")
  @Test
  void rejectsRefreshTokenShorterThan43Characters() {
    assertViolation(new RefreshRequest("A".repeat(42)), "refreshToken", "43자이어야 합니다.");
  }

  @DisplayName("[M1-DTO-07] rejectsRefreshTokenLongerThan43Characters")
  @Test
  void rejectsRefreshTokenLongerThan43Characters() {
    assertViolation(new RefreshRequest("A".repeat(44)), "refreshToken", "43자이어야 합니다.");
  }

  @DisplayName("[M1-DTO-07] rejectsRefreshTokenWithNonBase64UrlCharacter")
  @Test
  void rejectsRefreshTokenWithNonBase64UrlCharacter() {
    assertViolation(
        new RefreshRequest("+" + "A".repeat(42)), "refreshToken", "Base64 URL 형식이어야 합니다.");
  }

  @DisplayName("[M1-DTO-07] rejectsMissingRefreshToken")
  @Test
  void rejectsMissingRefreshToken() {
    assertViolation(new RefreshRequest(null), "refreshToken", "필수입니다.");
  }

  @DisplayName("[M1-DTO-07] rejectsEmptyRefreshToken")
  @Test
  void rejectsEmptyRefreshToken() {
    assertViolation(new RefreshRequest(""), "refreshToken", "43자이어야 합니다.");
  }

  @DisplayName("[M1-DTO-05] rejectsMissingLoginEmail")
  @Test
  void rejectsMissingLoginEmail() {
    assertViolation(new LoginRequest(null, "StringPass123!"), "email", "필수입니다.");
  }

  @DisplayName("[M1-DTO-05] rejectsBlankLoginEmail")
  @Test
  void rejectsBlankLoginEmail() {
    assertViolation(new LoginRequest("", "StringPass123!"), "email", "필수입니다.");
  }

  @DisplayName("[M1-DTO-05] rejectsLoginEmailBlankAfterNormalization")
  @Test
  void rejectsLoginEmailBlankAfterNormalization() {
    assertViolation(new LoginRequest("  ", "StringPass123!"), "email", "필수입니다.");
  }

  @DisplayName("[M1-DTO-05] rejectsInvalidLoginEmail")
  @Test
  void rejectsInvalidLoginEmail() {
    assertViolation(
        new LoginRequest("not-an-email", "StringPass123!"), "email", "올바른 이메일 형식이어야 합니다.");
  }

  @DisplayName("[M1-DTO-05] rejectsLoginEmailShorterThanThreeCharacters")
  @Test
  void rejectsLoginEmailShorterThanThreeCharacters() {
    assertViolation(new LoginRequest("a@", "StringPass123!"), "email", "3자 이상 254자 이하이어야 합니다.");
  }

  @DisplayName("[M1-DTO-05] rejectsLoginEmailLongerThan254Characters")
  @Test
  void rejectsLoginEmailLongerThan254Characters() {
    assertViolation(
        new LoginRequest(longestValidEmail() + "x", "StringPass123!"),
        "email",
        "3자 이상 254자 이하이어야 합니다.");
  }

  @DisplayName("[M1-DTO-05] rejectsLoginEmailLongerThan254CharactersAfterNormalization")
  @Test
  void rejectsLoginEmailLongerThan254CharactersAfterNormalization() {
    assertViolation(
        new LoginRequest(" " + longestValidEmail() + " x", "StringPass123!"),
        "email",
        "3자 이상 254자 이하이어야 합니다.");
  }

  @DisplayName("[M1-DTO-07] acceptsRefreshTokenWithBase64UrlCharacters")
  @Test
  void acceptsRefreshTokenWithBase64UrlCharacters() {
    assertThat(violations(new RefreshRequest("A".repeat(41) + "-_"))).isEmpty();
  }

  @DisplayName("[M1-DTO-06] rejectsMissingLoginPassword")
  @Test
  void rejectsMissingLoginPassword() {
    assertViolation(new LoginRequest("guardian@example.com", null), "password", "필수입니다.");
  }

  @DisplayName("[M1-DTO-06] rejectsEmptyLoginPassword")
  @Test
  void rejectsEmptyLoginPassword() {
    assertViolation(
        new LoginRequest("guardian@example.com", ""), "password", "1자 이상 72자 이하이어야 합니다.");
  }

  @DisplayName("[M1-DTO-06] rejectsLoginPasswordLongerThan72Characters")
  @Test
  void rejectsLoginPasswordLongerThan72Characters() {
    assertViolation(
        new LoginRequest("guardian@example.com", "String123!" + "a".repeat(63)),
        "password",
        "1자 이상 72자 이하이어야 합니다.");
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

  private SignupRequest signupWithEmail(String email) {
    return new SignupRequest(
        email, "StrongPass123!", ProfileType.INDIVIDUAL, Gender.FEMALE, IdentityVisibility.PUBLIC);
  }

  private String longestValidEmail() {
    return "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61);
  }

  private <T> void assertViolation(T value, String field, String message) {
    assertThat(violations(value))
        .anySatisfy(
            violation -> {
              assertThat(violation.getPropertyPath().toString()).isEqualTo(field);
              assertThat(violation.getMessage()).isEqualTo(message);
            });
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

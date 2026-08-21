package io.github.jo0yo0n.mypetmate.guardian.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class SignupPasswordValidator
    implements ConstraintValidator<ValidSignupPassword, CharSequence> {

  private static final String ALLOWED_SPECIAL_CHARACTERS = "!@#$%^&*";

  @Override
  public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
    if (value == null || value.isEmpty()) {
      return true;
    }

    boolean hasUppercase = false;
    boolean hasLowercase = false;
    boolean hasDigit = false;
    boolean hasSpecial = false;

    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      if (character >= 'A' && character <= 'Z') {
        hasUppercase = true;
      } else if (character >= 'a' && character <= 'z') {
        hasLowercase = true;
      } else if (character >= '0' && character <= '9') {
        hasDigit = true;
      } else if (ALLOWED_SPECIAL_CHARACTERS.indexOf(character) >= 0) {
        hasSpecial = true;
      } else {
        return false;
      }
    }

    return hasUppercase && hasLowercase && hasDigit && hasSpecial;
  }
}

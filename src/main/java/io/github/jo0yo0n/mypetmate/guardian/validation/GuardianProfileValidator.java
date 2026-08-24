package io.github.jo0yo0n.mypetmate.guardian.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class GuardianProfileValidator
    implements ConstraintValidator<ValidGuardianProfile, GuardianProfileFields> {

  @Override
  public boolean isValid(GuardianProfileFields input, ConstraintValidatorContext context) {
    if (input == null || input.profileType() == null) {
      return true;
    }

    boolean valid = input.profileType().requiresGender() == (input.gender() != null);
    if (!valid) {
      context.disableDefaultConstraintViolation();
      String message =
          input.profileType().requiresGender()
              ? "profileType이 individual이면 필수입니다."
              : "profileType이 couple 또는 family이면 사용할 수 없습니다.";
      context
          .buildConstraintViolationWithTemplate(message)
          .addPropertyNode("gender")
          .addConstraintViolation();
    }
    return valid;
  }
}

package io.github.jo0yo0n.mypetmate.guardian.validation;

import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class GuardianProfileValidator
    implements ConstraintValidator<ValidGuardianProfile, GuardianProfileFields> {

  @Override
  public boolean isValid(GuardianProfileFields input, ConstraintValidatorContext context) {
    if (input == null || input.profileType() == null) {
      return true;
    }

    boolean valid = input.profileType() != ProfileType.INDIVIDUAL || input.gender() != null;
    if (!valid) {
      context.disableDefaultConstraintViolation();
      String message = "profileType이 individual이면 필수입니다.";
      context
          .buildConstraintViolationWithTemplate(message)
          .addPropertyNode("gender")
          .addConstraintViolation();
    }
    return valid;
  }
}

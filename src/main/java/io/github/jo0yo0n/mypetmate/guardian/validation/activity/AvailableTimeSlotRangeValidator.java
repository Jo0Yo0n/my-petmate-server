package io.github.jo0yo0n.mypetmate.guardian.validation.activity;

import io.github.jo0yo0n.mypetmate.guardian.dto.AvailableTimeSlotRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class AvailableTimeSlotRangeValidator
    implements ConstraintValidator<ValidAvailableTimeSlotRange, AvailableTimeSlotRequest> {

  @Override
  public boolean isValid(AvailableTimeSlotRequest input, ConstraintValidatorContext context) {
    if (input == null || !input.hasValidTimeFormat() || input.startMinute() < input.endMinute()) {
      return true;
    }

    context.disableDefaultConstraintViolation();
    context
        .buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
        .addPropertyNode("endTime")
        .addConstraintViolation();
    return false;
  }
}

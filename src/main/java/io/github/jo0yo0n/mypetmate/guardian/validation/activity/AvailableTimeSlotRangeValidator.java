package io.github.jo0yo0n.mypetmate.guardian.validation.activity;

import io.github.jo0yo0n.mypetmate.guardian.dto.AvailableTimeSlotRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public final class AvailableTimeSlotRangeValidator
    implements ConstraintValidator<ValidAvailableTimeSlotRange, AvailableTimeSlotRequest> {

  @Override
  public boolean isValid(AvailableTimeSlotRequest input, ConstraintValidatorContext context) {
    if (input == null || input.startTime() == null || input.endTime() == null) {
      return true;
    }

    LocalTime startTime;
    LocalTime endTime;
    try {
      startTime = LocalTime.parse(input.startTime());
      endTime = LocalTime.parse(input.endTime());
    } catch (DateTimeParseException exception) {
      // 필수값과 시각 형식은 DTO의 필드 제약(@Pattern)에서 검증한다.
      return true;
    }

    if (startTime.isBefore(endTime)) {
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

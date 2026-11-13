package io.github.jo0yo0n.mypetmate.guardian.validation.activity;

import io.github.jo0yo0n.mypetmate.guardian.domain.Weekday;
import io.github.jo0yo0n.mypetmate.guardian.dto.AvailableTimeSlotRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;

public final class NoOverlappingTimeSlotsValidator
    implements ConstraintValidator<NoOverlappingTimeSlots, GuardianActivityFields> {

  @Override
  public boolean isValid(GuardianActivityFields request, ConstraintValidatorContext context) {
    if (request == null) {
      return true;
    }

    List<AvailableTimeSlotRequest> slots = request.availableTimeSlots();
    if (slots == null || slots.size() < 2) {
      return true;
    }

    var slotsByDay = new EnumMap<Weekday, List<TimeRange>>(Weekday.class);
    for (AvailableTimeSlotRequest slot : slots) {
      if (slot == null
          || slot.dayOfWeek() == null
          || slot.startTime() == null
          || slot.endTime() == null) {
        continue;
      }

      try {
        LocalTime start = LocalTime.parse(slot.startTime());
        LocalTime end = LocalTime.parse(slot.endTime());
        if (start.isBefore(end)) {
          slotsByDay
              .computeIfAbsent(slot.dayOfWeek(), day -> new ArrayList<>())
              .add(new TimeRange(start, end));
        }
      } catch (DateTimeParseException exception) {
        // 시각 형식 오류는 슬롯의 필드 제약에서 처리한다.
      }
    }

    for (List<TimeRange> daySlots : slotsByDay.values()) {
      daySlots.sort(Comparator.comparing(TimeRange::start));
      for (int index = 1; index < daySlots.size(); index++) {
        TimeRange previous = daySlots.get(index - 1);
        TimeRange current = daySlots.get(index);
        if (current.start().isBefore(previous.end())) {
          context.disableDefaultConstraintViolation();
          context
              .buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
              .addPropertyNode("availableTimeSlots")
              .addConstraintViolation();
          return false;
        }
      }
    }
    return true;
  }

  private record TimeRange(LocalTime start, LocalTime end) {}
}

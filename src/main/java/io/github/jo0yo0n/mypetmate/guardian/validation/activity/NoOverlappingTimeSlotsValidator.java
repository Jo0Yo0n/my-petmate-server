package io.github.jo0yo0n.mypetmate.guardian.validation.activity;

import io.github.jo0yo0n.mypetmate.guardian.domain.Weekday;
import io.github.jo0yo0n.mypetmate.guardian.dto.AvailableTimeSlotRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
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
      if (slot == null || slot.dayOfWeek() == null || !slot.hasValidTimeFormat()) {
        continue;
      }

      int start = slot.startMinute();
      int end = slot.endMinute();
      if (start < end) {
        slotsByDay
            .computeIfAbsent(slot.dayOfWeek(), day -> new ArrayList<>())
            .add(new TimeRange(start, end));
      }
    }

    for (List<TimeRange> daySlots : slotsByDay.values()) {
      daySlots.sort(Comparator.comparingInt(TimeRange::start));
      for (int index = 1; index < daySlots.size(); index++) {
        TimeRange previous = daySlots.get(index - 1);
        TimeRange current = daySlots.get(index);
        if (current.start() < previous.end()) {
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

  private record TimeRange(int start, int end) {}
}

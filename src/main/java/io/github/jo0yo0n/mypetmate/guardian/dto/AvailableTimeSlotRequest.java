package io.github.jo0yo0n.mypetmate.guardian.dto;

import io.github.jo0yo0n.mypetmate.guardian.domain.ActivityTimeSlot;
import io.github.jo0yo0n.mypetmate.guardian.domain.Weekday;
import io.github.jo0yo0n.mypetmate.guardian.validation.activity.ValidAvailableTimeSlotRange;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

@ValidAvailableTimeSlotRange
public record AvailableTimeSlotRequest(
    @NotNull(message = "필수입니다.") Weekday dayOfWeek,
    @NotNull(message = "필수입니다.")
        @Pattern(regexp = START_TIME_FORMAT, message = "10분 단위 HH:mm 형식이어야 합니다.")
        String startTime,
    @NotNull(message = "필수입니다.")
        @Pattern(regexp = END_TIME_FORMAT, message = "10분 단위 HH:mm 형식이어야 합니다.")
        String endTime) {

  private static final String START_TIME_FORMAT = "([01][0-9]|2[0-3]):[0-5]0";
  private static final String END_TIME_FORMAT = "(?:" + START_TIME_FORMAT + "|24:00)";
  private static final java.util.regex.Pattern START_PATTERN =
      java.util.regex.Pattern.compile(START_TIME_FORMAT);
  private static final java.util.regex.Pattern END_PATTERN =
      java.util.regex.Pattern.compile(END_TIME_FORMAT);

  public boolean hasValidTimeFormat() {
    return startTime != null
        && START_PATTERN.matcher(startTime).matches()
        && endTime != null
        && END_PATTERN.matcher(endTime).matches();
  }

  public int startMinute() {
    return toMinuteOfDay(startTime);
  }

  public int endMinute() {
    return toMinuteOfDay(endTime);
  }

  public static List<ActivityTimeSlot> toActivityTimeSlots(List<AvailableTimeSlotRequest> slots) {
    return slots == null
        ? null
        : slots.stream().map(AvailableTimeSlotRequest::toActivityTimeSlot).toList();
  }

  private ActivityTimeSlot toActivityTimeSlot() {
    return new ActivityTimeSlot(dayOfWeek, startMinute(), endMinute());
  }

  private static int toMinuteOfDay(String time) {
    return Integer.parseInt(time, 0, 2, 10) * 60 + Integer.parseInt(time, 3, 5, 10);
  }
}

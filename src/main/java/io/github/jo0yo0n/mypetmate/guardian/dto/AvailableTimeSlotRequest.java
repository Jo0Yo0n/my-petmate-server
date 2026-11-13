package io.github.jo0yo0n.mypetmate.guardian.dto;

import io.github.jo0yo0n.mypetmate.guardian.domain.Weekday;
import io.github.jo0yo0n.mypetmate.guardian.validation.activity.ValidAvailableTimeSlotRange;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@ValidAvailableTimeSlotRange
public record AvailableTimeSlotRequest(
    @NotNull(message = "필수입니다.") Weekday dayOfWeek,
    @NotNull(message = "필수입니다.")
        @Pattern(regexp = "([01][0-9]|2[0-3]):[0-5]0", message = "10분 단위 HH:mm 형식이어야 합니다.")
        String startTime,
    @NotNull(message = "필수입니다.")
        @Pattern(regexp = "([01][0-9]|2[0-3]):[0-5]0", message = "10분 단위 HH:mm 형식이어야 합니다.")
        String endTime) {}

package io.github.jo0yo0n.mypetmate.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jo0yo0n.mypetmate.guardian.domain.DiscoveryStatus;
import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import io.github.jo0yo0n.mypetmate.guardian.domain.Weekday;
import io.github.jo0yo0n.mypetmate.guardian.dto.AvailableTimeSlotRequest;
import jakarta.validation.Validation;
import java.util.List;
import org.junit.jupiter.api.Test;

class SignupTimeSlotValidationTest {

  @Test
  void rejectsOverlappingSlotsOnTheSameDay() {
    var request =
        signup(
            new AvailableTimeSlotRequest(Weekday.MONDAY, "10:00", "12:00"),
            new AvailableTimeSlotRequest(Weekday.TUESDAY, "09:30", "10:30"),
            new AvailableTimeSlotRequest(Weekday.MONDAY, "09:00", "11:00"));

    try (var factory = Validation.buildDefaultValidatorFactory()) {
      assertThat(factory.getValidator().validate(request))
          .singleElement()
          .satisfies(
              violation ->
                  assertThat(violation.getPropertyPath().toString())
                      .isEqualTo("availableTimeSlots"));
    }
  }

  @Test
  void acceptsAdjacentSlotsAndOverlapsOnDifferentDays() {
    var request =
        signup(
            new AvailableTimeSlotRequest(Weekday.MONDAY, "10:00", "11:00"),
            new AvailableTimeSlotRequest(Weekday.TUESDAY, "09:30", "10:30"),
            new AvailableTimeSlotRequest(Weekday.MONDAY, "09:00", "10:00"));

    try (var factory = Validation.buildDefaultValidatorFactory()) {
      assertThat(factory.getValidator().validate(request)).isEmpty();
    }
  }

  private SignupRequest signup(AvailableTimeSlotRequest... slots) {
    return new SignupRequest(
        "보호자",
        "guardian@example.com",
        "StrongPass123!",
        ProfileType.FAMILY,
        null,
        DiscoveryStatus.PAUSED,
        List.of(),
        List.of(slots));
  }
}

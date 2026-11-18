package io.github.jo0yo0n.mypetmate.guardian.validation.activity;

import io.github.jo0yo0n.mypetmate.guardian.domain.DiscoveryStatus;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class SearchingActivityRequiredFieldsValidator
    implements ConstraintValidator<RequiresActivityFieldsWhenSearching, GuardianActivityFields> {

  @Override
  public boolean isValid(GuardianActivityFields request, ConstraintValidatorContext context) {

    // 요청·상태의 null 검증은 각각 @NotNull에서 담당한다.
    if (request == null || request.discoveryStatus() != DiscoveryStatus.SEARCHING) {
      return true;
    }

    boolean regionMissing =
        request.activityRegionIds() == null || request.activityRegionIds().isEmpty();
    boolean slotsMissing =
        request.availableTimeSlots() == null || request.availableTimeSlots().isEmpty();

    if (!regionMissing && !slotsMissing) {
      return true;
    }

    context.disableDefaultConstraintViolation();

    if (regionMissing) {
      context
          .buildConstraintViolationWithTemplate("활동 지역을 1개 이상 선택해주세요.")
          .addPropertyNode("activityRegionIds")
          .addConstraintViolation();
    }

    if (slotsMissing) {
      context
          .buildConstraintViolationWithTemplate("활동 시간을 1개 이상 등록해주세요.")
          .addPropertyNode("availableTimeSlots")
          .addConstraintViolation();
    }

    return false;
  }
}

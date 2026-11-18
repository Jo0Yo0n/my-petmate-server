package io.github.jo0yo0n.mypetmate.auth.exception;

import io.github.jo0yo0n.mypetmate.error.FieldValidationException;
import io.github.jo0yo0n.mypetmate.region.persistence.entity.Region;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class InvalidActivityRegionException extends FieldValidationException {
  private static final String REASON = "존재하지 않는 읍·면·동 입니다.";

  public InvalidActivityRegionException(List<UUID> requestedIds, List<Region> selectableRegions) {
    super(REASON, invalidFields(requestedIds, selectableRegions));
  }

  private static List<FieldError> invalidFields(
      List<UUID> requestedIds, List<Region> selectableRegions) {
    var selectableIds = selectableRegions.stream().map(Region::getId).collect(Collectors.toSet());
    var errors = new ArrayList<FieldError>();
    for (int index = 0; index < requestedIds.size(); index++) {
      if (!selectableIds.contains(requestedIds.get(index))) {
        errors.add(new FieldError("activityRegionIds[" + index + "]", REASON));
      }
    }
    return errors;
  }
}

package io.github.jo0yo0n.mypetmate.guardian.validation.activity;

import io.github.jo0yo0n.mypetmate.guardian.domain.DiscoveryStatus;
import io.github.jo0yo0n.mypetmate.guardian.dto.AvailableTimeSlotRequest;
import java.util.List;
import java.util.UUID;

public interface GuardianActivityFields {

  DiscoveryStatus discoveryStatus();

  List<UUID> activityRegionIds();

  List<AvailableTimeSlotRequest> availableTimeSlots();
}

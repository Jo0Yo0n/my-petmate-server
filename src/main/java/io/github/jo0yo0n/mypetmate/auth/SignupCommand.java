package io.github.jo0yo0n.mypetmate.auth;

import io.github.jo0yo0n.mypetmate.guardian.domain.ActivityTimeSlot;
import io.github.jo0yo0n.mypetmate.guardian.domain.DiscoveryStatus;
import io.github.jo0yo0n.mypetmate.guardian.domain.Gender;
import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import java.util.List;
import java.util.UUID;

record SignupCommand(
    String nickname,
    String email,
    String password,
    ProfileType profileType,
    Gender gender,
    DiscoveryStatus discoveryStatus,
    List<UUID> activityRegionIds,
    List<ActivityTimeSlot> availableTimeSlots) {}

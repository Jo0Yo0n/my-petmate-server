package io.github.jo0yo0n.mypetmate.guardian.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.jo0yo0n.mypetmate.guardian.domain.DiscoveryStatus;
import io.github.jo0yo0n.mypetmate.guardian.domain.Gender;
import io.github.jo0yo0n.mypetmate.guardian.domain.GuardianStatus;
import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import io.github.jo0yo0n.mypetmate.guardian.domain.Weekday;
import io.github.jo0yo0n.mypetmate.guardian.persistence.entity.Guardian;
import io.github.jo0yo0n.mypetmate.guardian.persistence.entity.GuardianAvailableTimeSlot;
import io.github.jo0yo0n.mypetmate.region.domain.RegionLevel;
import io.github.jo0yo0n.mypetmate.region.persistence.entity.Region;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public record GuardianResponse(
    UUID id,
    String nickname,
    String email,
    ProfileType profileType,
    Gender gender,
    DiscoveryStatus discoveryStatus,
    GuardianStatus status,
    @JsonInclude(JsonInclude.Include.ALWAYS) String photoUrl,
    List<ActivityRegion> activityRegions,
    List<AvailableTimeSlot> availableTimeSlots) {

  private static final DateTimeFormatter SLOT_TIME =
      DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

  public static GuardianResponse from(
      Guardian guardian, List<Region> regions, List<GuardianAvailableTimeSlot> slots) {
    return new GuardianResponse(
        guardian.getId(),
        guardian.getNickname(),
        guardian.getEmail(),
        guardian.getProfileType(),
        guardian.getGender(),
        guardian.getDiscoveryStatus(),
        guardian.getStatus(),
        null,
        regions.stream().map(ActivityRegion::from).toList(),
        slots.stream().map(AvailableTimeSlot::from).toList());
  }

  public record ActivityRegion(UUID id, List<RegionPathSegment> path) {
    private static ActivityRegion from(Region region) {
      var path = new ArrayDeque<RegionPathSegment>();
      for (Region current = region; current != null; current = current.getParent()) {
        path.addFirst(new RegionPathSegment(current.getLevel(), current.getName()));
      }
      return new ActivityRegion(region.getId(), List.copyOf(path));
    }
  }

  public record RegionPathSegment(RegionLevel level, String name) {}

  public record AvailableTimeSlot(Weekday dayOfWeek, String startTime, String endTime) {
    private static AvailableTimeSlot from(GuardianAvailableTimeSlot slot) {
      return new AvailableTimeSlot(
          slot.getDayOfWeek(),
          slot.getStartTime().format(SLOT_TIME),
          slot.getEndTime().format(SLOT_TIME));
    }
  }
}

package io.github.jo0yo0n.mypetmate.guardian.persistence.entity;

import io.github.jo0yo0n.mypetmate.region.persistence.entity.Region;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "guardian_activity_region")
public class GuardianActivityRegion {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "guardian_id", nullable = false)
  private Guardian guardian;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "region_id", nullable = false)
  private Region region;

  protected GuardianActivityRegion() {}

  public GuardianActivityRegion(UUID id, Guardian guardian, Region region) {
    this.id = id;
    this.guardian = guardian;
    this.region = region;
  }

  public UUID getId() {
    return id;
  }

  public Guardian getGuardian() {
    return guardian;
  }

  public Region getRegion() {
    return region;
  }
}

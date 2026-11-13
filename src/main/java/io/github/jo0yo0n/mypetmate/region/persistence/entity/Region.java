package io.github.jo0yo0n.mypetmate.region.persistence.entity;

import io.github.jo0yo0n.mypetmate.region.domain.RegionLevel;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "region")
public class Region {
  @Id private UUID id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 16)
  private RegionLevel level;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private Region parent;

  protected Region() {}

  public Region(UUID id, String name, RegionLevel level, Region parent) {
    this.id = id;
    this.name = name;
    this.level = level;
    this.parent = parent;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public RegionLevel getLevel() {
    return level;
  }

  public Region getParent() {
    return parent;
  }
}

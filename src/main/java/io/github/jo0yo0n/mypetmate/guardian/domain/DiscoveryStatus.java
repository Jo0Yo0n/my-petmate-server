package io.github.jo0yo0n.mypetmate.guardian.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DiscoveryStatus {
  SEARCHING("searching"),
  PAUSED("paused");

  private final String value;

  DiscoveryStatus(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  @JsonCreator
  public static DiscoveryStatus fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (DiscoveryStatus discoveryStatus : values()) {
      if (discoveryStatus.value.equals(value)) {
        return discoveryStatus;
      }
    }
    throw new IllegalArgumentException("Unknown discovery status value");
  }
}

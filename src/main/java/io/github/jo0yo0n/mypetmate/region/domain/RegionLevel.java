package io.github.jo0yo0n.mypetmate.region.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RegionLevel {
  PROVINCE("province"),
  DISTRICT("district"),
  LOCALITY("locality");

  private final String value;

  RegionLevel(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  @JsonCreator
  public static RegionLevel fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (RegionLevel level : values()) {
      if (level.value.equals(value)) {
        return level;
      }
    }
    throw new IllegalArgumentException("Unknown region level value");
  }
}

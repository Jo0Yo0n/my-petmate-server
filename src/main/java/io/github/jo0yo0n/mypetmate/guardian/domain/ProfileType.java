package io.github.jo0yo0n.mypetmate.guardian.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ProfileType {
  INDIVIDUAL("individual"),
  COUPLE("couple"),
  FAMILY("family");

  private final String value;

  ProfileType(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  @JsonCreator
  public static ProfileType fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (ProfileType profileType : values()) {
      if (profileType.value.equals(value)) {
        return profileType;
      }
    }
    throw new IllegalArgumentException("Unknown profile type value");
  }
}

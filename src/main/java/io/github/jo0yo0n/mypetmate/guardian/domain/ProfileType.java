package io.github.jo0yo0n.mypetmate.guardian.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ProfileType {
  INDIVIDUAL("individual", true),
  COUPLE("couple", false),
  FAMILY("family", false);

  private final String value;
  private final boolean genderRequired;

  ProfileType(String value, boolean genderRequired) {
    this.value = value;
    this.genderRequired = genderRequired;
  }

  @JsonValue
  public String value() {
    return value;
  }

  public boolean requiresGender() {
    return genderRequired;
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

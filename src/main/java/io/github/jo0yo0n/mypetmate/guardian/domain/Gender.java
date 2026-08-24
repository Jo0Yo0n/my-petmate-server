package io.github.jo0yo0n.mypetmate.guardian.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Gender {
  FEMALE("female"),
  MALE("male");

  private final String value;

  Gender(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  @JsonCreator
  public static Gender fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (Gender gender : values()) {
      if (gender.value.equals(value)) {
        return gender;
      }
    }
    throw new IllegalArgumentException("Unknown gender value");
  }
}

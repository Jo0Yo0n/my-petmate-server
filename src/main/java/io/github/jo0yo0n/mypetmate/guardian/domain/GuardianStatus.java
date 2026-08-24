package io.github.jo0yo0n.mypetmate.guardian.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum GuardianStatus {
  ACTIVE("active"),
  TEMPORARILY_RESTRICTED("temporarily_restricted"),
  WITHDRAWN("withdrawn");

  private final String value;

  GuardianStatus(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  @JsonCreator
  public static GuardianStatus fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (GuardianStatus status : values()) {
      if (status.value.equals(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown guardian status value");
  }
}

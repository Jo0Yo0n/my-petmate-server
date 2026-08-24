package io.github.jo0yo0n.mypetmate.guardian.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum IdentityVisibility {
  PUBLIC("public"),
  PRIVATE("private");

  private final String value;

  IdentityVisibility(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  @JsonCreator
  public static IdentityVisibility fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (IdentityVisibility visibility : values()) {
      if (visibility.value.equals(value)) {
        return visibility;
      }
    }
    throw new IllegalArgumentException("Unknown identity visibility value");
  }
}

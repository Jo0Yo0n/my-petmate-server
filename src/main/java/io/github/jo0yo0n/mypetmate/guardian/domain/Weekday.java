package io.github.jo0yo0n.mypetmate.guardian.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Weekday {
  MONDAY("mon"),
  TUESDAY("tue"),
  WEDNESDAY("wed"),
  THURSDAY("thu"),
  FRIDAY("fri"),
  SATURDAY("sat"),
  SUNDAY("sun");

  private final String value;

  Weekday(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  @JsonCreator
  public static Weekday fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (Weekday weekday : values()) {
      if (weekday.value.equals(value)) {
        return weekday;
      }
    }
    throw new IllegalArgumentException("Unknown weekday value");
  }
}

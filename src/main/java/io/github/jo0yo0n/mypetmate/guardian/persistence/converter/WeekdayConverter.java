package io.github.jo0yo0n.mypetmate.guardian.persistence.converter;

import io.github.jo0yo0n.mypetmate.guardian.domain.Weekday;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class WeekdayConverter implements AttributeConverter<Weekday, String> {
  @Override
  public String convertToDatabaseColumn(Weekday attribute) {
    return attribute == null ? null : attribute.value();
  }

  @Override
  public Weekday convertToEntityAttribute(String dbData) {
    return Weekday.fromValue(dbData);
  }
}

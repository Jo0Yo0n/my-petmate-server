package io.github.jo0yo0n.mypetmate.guardian.persistence;

import io.github.jo0yo0n.mypetmate.guardian.domain.Gender;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class GenderConverter implements AttributeConverter<Gender, String> {

  @Override
  public String convertToDatabaseColumn(Gender attribute) {
    return attribute == null ? null : attribute.value();
  }

  @Override
  public Gender convertToEntityAttribute(String dbData) {
    return Gender.fromValue(dbData);
  }
}

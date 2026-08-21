package io.github.jo0yo0n.mypetmate.guardian.persistence;

import io.github.jo0yo0n.mypetmate.guardian.domain.GuardianStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class GuardianStatusConverter implements AttributeConverter<GuardianStatus, String> {

  @Override
  public String convertToDatabaseColumn(GuardianStatus attribute) {
    return attribute == null ? null : attribute.value();
  }

  @Override
  public GuardianStatus convertToEntityAttribute(String dbData) {
    return GuardianStatus.fromValue(dbData);
  }
}

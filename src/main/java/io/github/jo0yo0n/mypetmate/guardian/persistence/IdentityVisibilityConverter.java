package io.github.jo0yo0n.mypetmate.guardian.persistence;

import io.github.jo0yo0n.mypetmate.guardian.domain.IdentityVisibility;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class IdentityVisibilityConverter implements AttributeConverter<IdentityVisibility, String> {

  @Override
  public String convertToDatabaseColumn(IdentityVisibility attribute) {
    return attribute == null ? null : attribute.value();
  }

  @Override
  public IdentityVisibility convertToEntityAttribute(String dbData) {
    return IdentityVisibility.fromValue(dbData);
  }
}

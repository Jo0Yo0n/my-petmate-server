package io.github.jo0yo0n.mypetmate.guardian.persistence;

import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProfileTypeConverter implements AttributeConverter<ProfileType, String> {

  @Override
  public String convertToDatabaseColumn(ProfileType attribute) {
    return attribute == null ? null : attribute.value();
  }

  @Override
  public ProfileType convertToEntityAttribute(String dbData) {
    return ProfileType.fromValue(dbData);
  }
}

package io.github.jo0yo0n.mypetmate.region.persistence.converter;

import io.github.jo0yo0n.mypetmate.region.domain.RegionLevel;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RegionLevelConverter implements AttributeConverter<RegionLevel, String> {
  @Override
  public String convertToDatabaseColumn(RegionLevel attribute) {
    return attribute == null ? null : attribute.value();
  }

  @Override
  public RegionLevel convertToEntityAttribute(String dbData) {
    return RegionLevel.fromValue(dbData);
  }
}

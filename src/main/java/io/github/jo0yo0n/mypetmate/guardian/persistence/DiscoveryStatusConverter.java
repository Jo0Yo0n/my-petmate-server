package io.github.jo0yo0n.mypetmate.guardian.persistence;

import io.github.jo0yo0n.mypetmate.guardian.domain.DiscoveryStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DiscoveryStatusConverter implements AttributeConverter<DiscoveryStatus, String> {

  @Override
  public String convertToDatabaseColumn(DiscoveryStatus attribute) {
    return attribute == null ? null : attribute.value();
  }

  @Override
  public DiscoveryStatus convertToEntityAttribute(String dbData) {
    return DiscoveryStatus.fromValue(dbData);
  }
}

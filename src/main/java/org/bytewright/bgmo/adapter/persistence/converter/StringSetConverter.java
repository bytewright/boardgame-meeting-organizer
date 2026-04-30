package org.bytewright.bgmo.adapter.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Converter
public class StringSetConverter implements AttributeConverter<Set<String>, String> {
  protected final JsonMapper objectMapper;

  public StringSetConverter() {
    objectMapper = JsonMapper.builder().findAndAddModules().build();
  }

  @Override
  public String convertToDatabaseColumn(Set<String> attribute) {
    if (attribute == null) {
      return null;
    }
    List<String> sortedEntries = attribute.stream().sorted(Comparator.naturalOrder()).toList();
    return objectMapper.writeValueAsString(sortedEntries);
  }

  @Override
  public Set<String> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.trim().isEmpty()) {
      return new HashSet<>();
    }
    List<String> sortedEntries = objectMapper.readValue(dbData, new TypeReference<>() {});
    return new HashSet<>(sortedEntries);
  }
}

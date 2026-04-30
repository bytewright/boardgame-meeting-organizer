package org.bytewright.bgmo.adapter.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {
  protected final JsonMapper objectMapper;

  public StringListConverter() {
    objectMapper = JsonMapper.builder().findAndAddModules().build();
  }

  @Override
  public String convertToDatabaseColumn(List<String> attribute) {
    if (attribute == null) {
      return null;
    }
    return objectMapper.writeValueAsString(attribute);
  }

  @Override
  public List<String> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.trim().isEmpty()) {
      return new ArrayList<>(0);
    }
    List<String> sortedEntries = objectMapper.readValue(dbData, new TypeReference<>() {});
    return new ArrayList<>(sortedEntries);
  }
}

package org.bytewright.bgmo.adapter.persistance.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Converter
public class StringSetConverter implements AttributeConverter<Set<String>, String> {
  protected final ObjectMapper objectMapper;

  public StringSetConverter() {
    objectMapper = new ObjectMapper();
  }

  @Override
  public String convertToDatabaseColumn(Set<String> attribute) {
    if (attribute == null) {
      return null;
    }
    List<String> sortedEntries = attribute.stream().sorted(Comparator.naturalOrder()).toList();
    try {
      return objectMapper.writeValueAsString(sortedEntries);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(
          "Could not convert Set<String> to JSON string: %s".formatted(attribute), e);
    }
  }

  @Override
  public Set<String> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.trim().isEmpty()) {
      return new HashSet<>();
    }
    try {
      List<String> sortedEntries = objectMapper.readValue(dbData, new TypeReference<>() {});
      return new HashSet<>(sortedEntries);
    } catch (IOException e) {
      throw new RuntimeException(
          "Could not convert JSON string to Set<String>: %s".formatted(dbData), e);
    }
  }
}

package org.bytewright.bgmo.adapter.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;
import java.util.*;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {
  protected final ObjectMapper objectMapper;

  public StringListConverter() {
    objectMapper = new ObjectMapper();
  }

  @Override
  public String convertToDatabaseColumn(List<String> attribute) {
    if (attribute == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(
          "Could not convert List<String> to JSON string: %s".formatted(attribute), e);
    }
  }

  @Override
  public List<String> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.trim().isEmpty()) {
      return new ArrayList<>(0);
    }
    try {
      List<String> sortedEntries = objectMapper.readValue(dbData, new TypeReference<>() {});
      return new ArrayList<>(sortedEntries);
    } catch (IOException e) {
      throw new RuntimeException(
          "Could not convert JSON string to List<String>: %s".formatted(dbData), e);
    }
  }
}

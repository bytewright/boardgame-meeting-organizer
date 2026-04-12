package org.bytewright.bgmo.adapter.persistance.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;
import java.util.*;

@Converter
public abstract class JsonConverter<KEYTYPE extends Comparable<KEYTYPE>, VALUETYPE>
    implements AttributeConverter<Map<KEYTYPE, VALUETYPE>, String> {
  protected final ObjectMapper objectMapper;
  protected final MapType mapType;

  public JsonConverter() {
    objectMapper = new ObjectMapper();
    getCustomModule().ifPresent(objectMapper::registerModule);
    this.mapType =
        objectMapper
            .getTypeFactory()
            .constructMapType(LinkedHashMap.class, keyclass(), valueClass());
  }

  protected Optional<Module> getCustomModule() {
    return Optional.empty();
  }

  protected abstract Class<KEYTYPE> keyclass();

  protected abstract Class<VALUETYPE> valueClass();

  @Override
  public String convertToDatabaseColumn(Map<KEYTYPE, VALUETYPE> attribute) {
    if (attribute == null) {
      return null;
    }
    try {
      List<Map.Entry<KEYTYPE, VALUETYPE>> sortedEntries =
          attribute.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
      LinkedHashMap<KEYTYPE, VALUETYPE> sortedMap = new LinkedHashMap<>();
      for (Map.Entry<KEYTYPE, VALUETYPE> entry : sortedEntries) {
        sortedMap.put(entry.getKey(), entry.getValue());
      }
      return objectMapper.writeValueAsString(sortedMap);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(
          "Could not convert Map<%s, %s> to JSON string".formatted(keyclass(), valueClass()), e);
    }
  }

  @Override
  public Map<KEYTYPE, VALUETYPE> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.trim().isEmpty()) {
      return Collections.emptyMap();
    }
    try {
      return objectMapper.<LinkedHashMap<KEYTYPE, VALUETYPE>>readValue(dbData, mapType);
    } catch (IOException e) {
      throw new RuntimeException(
          "Could not convert JSON string to Map<%s, %s>".formatted(keyclass(), valueClass()), e);
    }
  }
}

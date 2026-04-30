package org.bytewright.bgmo.adapter.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.*;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.MapType;

@Converter
public abstract class JsonConverter<KEYTYPE extends Comparable<KEYTYPE>, VALUETYPE>
    implements AttributeConverter<Map<KEYTYPE, VALUETYPE>, String> {
  protected final JsonMapper objectMapper;
  protected final MapType mapType;

  public JsonConverter() {
    JsonMapper.Builder builder = JsonMapper.builder().findAndAddModules();
    getCustomModule().ifPresent(builder::addModule);
    objectMapper = builder.build();
    this.mapType =
        objectMapper
            .getTypeFactory()
            .constructMapType(LinkedHashMap.class, keyclass(), valueClass());
  }

  protected Optional<JacksonModule> getCustomModule() {
    return Optional.empty();
  }

  protected abstract Class<KEYTYPE> keyclass();

  protected abstract Class<VALUETYPE> valueClass();

  @Override
  public String convertToDatabaseColumn(Map<KEYTYPE, VALUETYPE> attribute) {
    if (attribute == null) {
      return null;
    }
    List<Map.Entry<KEYTYPE, VALUETYPE>> sortedEntries =
        attribute.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
    LinkedHashMap<KEYTYPE, VALUETYPE> sortedMap = new LinkedHashMap<>();
    for (Map.Entry<KEYTYPE, VALUETYPE> entry : sortedEntries) {
      sortedMap.put(entry.getKey(), entry.getValue());
    }
    return objectMapper.writeValueAsString(sortedMap);
  }

  @Override
  public Map<KEYTYPE, VALUETYPE> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.trim().isEmpty()) {
      return Collections.emptyMap();
    }
    return objectMapper.<LinkedHashMap<KEYTYPE, VALUETYPE>>readValue(dbData, mapType);
  }
}

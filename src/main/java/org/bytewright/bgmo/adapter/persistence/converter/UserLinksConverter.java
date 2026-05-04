package org.bytewright.bgmo.adapter.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.List;
import org.bytewright.bgmo.domain.model.Game;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Converter
public class UserLinksConverter implements AttributeConverter<List<Game.UserLink>, String> {
  protected final JsonMapper mapper;

  public UserLinksConverter() {
    mapper = JsonMapper.builder().findAndAddModules().build();
  }

  @Override
  public String convertToDatabaseColumn(List<Game.UserLink> attribute) {
    if (attribute == null) {
      return null;
    }
    return mapper.writeValueAsString(attribute);
  }

  @Override
  public List<Game.UserLink> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.trim().isEmpty()) {
      return new ArrayList<>(0);
    }
    List<Game.UserLink> sortedEntries = mapper.readValue(dbData, new TypeReference<>() {});
    return new ArrayList<>(sortedEntries);
  }
}

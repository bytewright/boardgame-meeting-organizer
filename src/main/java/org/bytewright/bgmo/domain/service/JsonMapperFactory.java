package org.bytewright.bgmo.domain.service;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.json.JsonMapper;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonMapperFactory {
  public static JsonMapper unRedactedMapper() {
    return JsonMapper.builder()
        .findAndAddModules()
        .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
        .build();
  }
}

package org.bytewright.bgmo.domain.service;

import java.net.URI;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.AdapterSettings;
import org.bytewright.bgmo.domain.service.data.AdapterSettingsDao;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteManagementService implements AdapterSettingsProvider {
  private final JsonMapper mapper = JsonMapperFactory.unRedactedMapper();
  private final AdapterSettingsDao adapterSettingsDao;

  public URI getBaseUrl() {
    return URI.create(getSettings().getBaseUrl());
  }

  private SiteSettings getSettings() {
    AdapterSettings settings = adapterSettingsDao.findByAdapterName(getAdapterName());
    String adapterSettings = settings.getAdapterSettings();
    return mapper.readValue(adapterSettings, SiteSettings.class);
  }

  @Override
  public String getAdapterName() {
    return "SiteManagementService";
  }

  @Override
  public boolean isValidSettingsJson(String jsonData) {
    try {
      var settings = mapper.readValue(jsonData, SiteSettings.class);
      return settings != null;
    } catch (JacksonException e) {
      log.error("Provided settings are invalid", e);
    }
    return false;
  }

  @Override
  public String getDefaultSettings() throws Exception {
    return mapper.writeValueAsString(SiteSettings.builder().build());
  }

  @Data
  @Builder
  @Jacksonized
  private static class SiteSettings {
    @Builder.Default private String baseUrl = "";
  }
}

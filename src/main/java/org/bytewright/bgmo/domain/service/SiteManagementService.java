package org.bytewright.bgmo.domain.service;

import java.net.URI;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.AdapterSettings;
import org.bytewright.bgmo.domain.model.notification.NotificationTargetType;
import org.bytewright.bgmo.domain.service.data.AdapterSettingsDao;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteManagementService implements AdapterSettingsProvider {
  private static final String ADAPTER_NAME = "core.site-management-service";
  private final JsonMapper mapper = JsonMapperFactory.unRedactedMapper();
  private final AdapterSettingsDao adapterSettingsDao;

  public URI getBaseUrl() {
    return URI.create(getSettings().getBaseUrl());
  }

  private SiteSettings getSettings() {
    AdapterSettings settings = adapterSettingsDao.findByAdapter(getAdapterInfo());
    String adapterSettings = settings.getAdapterSettings();
    return mapper.readValue(adapterSettings, SiteSettings.class);
  }

  @Override
  public AdapterInfo getAdapterInfo() {
    return AdapterInfo.builder()
        .stableName(ADAPTER_NAME)
        .description("Global site-wide settings management")
        .build();
  }

  @Override
  public ValidationResult isValidSettingsJson(String jsonData) {
    try {
      var settings = mapper.readValue(jsonData, SiteSettings.class);
      ZoneId zoneId = ZoneId.of(settings.getTimeZone());
      log.debug("Zone seems to be valid as no exception thrown: {}", zoneId);
      URI uri = URI.create(settings.getBaseUrl());
      log.debug("Baseurl seems to be valid as no exception thrown: {}", uri);
      return ValidationResult.VALID;
    } catch (JacksonException e) {
      log.error("Error while validating json: {}", e.getMessage());
    }
    return ValidationResult.INVALID;
  }

  @Override
  public String getDefaultSettings() throws Exception {
    return mapper.writeValueAsString(SiteSettings.builder().build());
  }

  public ZoneId getServiceTimeZone() {
    return ZoneId.of(getSettings().getTimeZone());
  }

  public Locale getDefaultLocale() {
    return getDefaultLocale(NotificationTargetType.DIRECT);
  }

  public Locale getDefaultLocale(NotificationTargetType type) {
    Optional<SiteSettings> settings = Optional.ofNullable(getSettings());
    return switch (type) {
      case DIRECT -> settings.map(SiteSettings::getDefaultLocale).orElse(Locale.GERMAN);
      case GROUP -> settings.map(SiteSettings::getDefaultGroupLocale).orElse(Locale.GERMAN);
    };
  }

  @Data
  @Builder
  @Jacksonized
  private static class SiteSettings {
    @Builder.Default private String baseUrl = "http://localhost:8080/";
    @Builder.Default private String timeZone = "Europe/Berlin";
    @Builder.Default private Locale defaultLocale = Locale.GERMAN;
    @Builder.Default private Locale defaultGroupLocale = Locale.GERMAN;
  }
}

package org.bytewright.bgmo.domain.service;

import java.util.Optional;
import lombok.Builder;
import org.bytewright.bgmo.domain.model.AdapterSettings;

/**
 * Allows adapters to store data in DB, this is mainly for site wide settings, e.g. configurable
 * display names or URLs
 *
 * <p>Data is always stored as text, but content is completely adapter owned
 *
 * @see org.bytewright.bgmo.domain.model.AdapterSettings
 * @see org.bytewright.bgmo.domain.service.data.AdapterSettingsDao
 */
public interface AdapterSettingsProvider {
  /** Must be application context wide unique */
  AdapterInfo getAdapterInfo();

  /** Called when admins change data manually in frontend */
  ValidationResult isValidSettingsJson(String jsonData);

  String getDefaultSettings() throws Exception;

  default Optional<AdapterSettings> attemptSettingsRecovery(AdapterSettings adapterSettings) {
    try {
      adapterSettings.setAdapterSettings(getDefaultSettings());
      return Optional.of(adapterSettings);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Builder
  record AdapterInfo(String stableName, String description) {}

  enum ValidationResult {
    VALID,
    INVALID
  }
}

package org.bytewright.bgmo.domain.service;

import lombok.Builder;

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

  @Builder
  record AdapterInfo(String stableName, String description) {}

  enum ValidationResult {
    VALID,
    INVALID
  }
}

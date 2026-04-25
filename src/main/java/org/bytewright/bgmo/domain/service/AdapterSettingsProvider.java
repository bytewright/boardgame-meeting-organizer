package org.bytewright.bgmo.domain.service;

public interface AdapterSettingsProvider {
  String getAdapterName();

  boolean isValidSettingsJson(String jsonData);

  String getDefaultSettings() throws Exception;
}

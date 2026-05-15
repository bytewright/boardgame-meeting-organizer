package org.bytewright.bgmo.usecases;

import static org.bytewright.bgmo.domain.service.AdapterSettingsProvider.ValidationResult.*;

import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.AdapterSettings;
import org.bytewright.bgmo.domain.service.AdapterSettingsProvider;
import org.bytewright.bgmo.domain.service.data.AdapterSettingsDao;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AdapterSettingsWorkflows implements ApplicationListener<ContextRefreshedEvent> {
  private final Set<AdapterSettingsProvider> adapterSettingsProviders;
  private final AdapterSettingsDao adapterSettingsDao;

  @Override
  public void onApplicationEvent(ContextRefreshedEvent ignored) {
    Set<String> seenNames = new HashSet<>();
    for (AdapterSettingsProvider provider : adapterSettingsProviders) {
      String adapterName = provider.getAdapterInfo().stableName();
      if (!seenNames.add(adapterName)) {
        throw new IllegalStateException("Detected two adapters with same name: " + adapterName);
      }
      if (!adapterSettingsDao.existsByAdapterName(adapterName)) {
        createDefaultSettings(provider);
      } else {
        var adapterSettings = adapterSettingsDao.findByAdapter(provider.getAdapterInfo());
        verifySettings(provider, adapterSettings, false);
      }
    }
  }

  private boolean verifySettings(
      AdapterSettingsProvider provider, AdapterSettings adapterSettings, boolean recovery) {
    try {
      var validationResult = provider.isValidSettingsJson(adapterSettings.getAdapterSettings());
      if (validationResult == VALID) return true;
    } catch (Exception e) {
      log.error(
          "Provider crashed during validation: {}", provider.getAdapterInfo().stableName(), e);
    }
    if (recovery) {
      log.error("Recovery of settings failed for {}", provider.getAdapterInfo().stableName());
      return false;
    }
    Optional<AdapterSettings> fallbackSettings = provider.attemptSettingsRecovery(adapterSettings);
    if (fallbackSettings.isEmpty()) {
      log.error(
          "Database contains incompatible adapter config for, recovery failed: {}",
          provider.getAdapterInfo());
      return false;
    }
    AdapterSettings settings = fallbackSettings.get();
    if (verifySettings(provider, adapterSettings, true)) {
      log.warn("Recovery successful for settings {}", provider.getAdapterInfo().stableName());
      adapterSettingsDao.createOrUpdate(settings);
      return true;
    }
    return false;
  }

  @SneakyThrows
  private void createDefaultSettings(AdapterSettingsProvider provider) {
    String defaultSettingsJson = provider.getDefaultSettings();
    if (provider.isValidSettingsJson(defaultSettingsJson) == INVALID) {
      throw new IllegalArgumentException(
          "Adapter returned invalid default settings! " + provider.getAdapterInfo());
    }
    AdapterSettings settings =
        AdapterSettings.builder()
            .adapterName(provider.getAdapterInfo().stableName())
            .adapterSettings(defaultSettingsJson)
            .build();
    log.info("Creating default settings in db for adapter: {}", provider.getAdapterInfo());
    adapterSettingsDao.createOrUpdate(settings);
  }
}

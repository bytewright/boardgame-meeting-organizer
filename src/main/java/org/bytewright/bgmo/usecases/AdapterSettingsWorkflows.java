package org.bytewright.bgmo.usecases;

import jakarta.transaction.Transactional;
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
    for (AdapterSettingsProvider provider : adapterSettingsProviders) {
      String adapterName = provider.getAdapterName();
      if (!adapterSettingsDao.existsByAdapterName(adapterName)) {
        createDefaultSettings(provider);
      } else {
        verifySettings(provider);
      }
    }
  }

  private void verifySettings(AdapterSettingsProvider provider) {
    AdapterSettings adapterSettings =
        adapterSettingsDao.findByAdapterName(provider.getAdapterName());
    if (!provider.isValidSettingsJson(adapterSettings.getAdapterSettings())) {
      throw new IllegalArgumentException(
          "Database contains incompatible adapter config for: " + provider.getAdapterName());
    }
  }

  @SneakyThrows
  private void createDefaultSettings(AdapterSettingsProvider provider) {
    String defaultSettingsJson = provider.getDefaultSettings();
    if (!provider.isValidSettingsJson(defaultSettingsJson)) {
      throw new IllegalArgumentException(
          "Adapter returned invalid default settings! " + provider.getAdapterName());
    }
    AdapterSettings settings =
        AdapterSettings.builder()
            .adapterName(provider.getAdapterName())
            .adapterSettings(defaultSettingsJson)
            .build();
    adapterSettingsDao.createOrUpdate(settings);
  }
}

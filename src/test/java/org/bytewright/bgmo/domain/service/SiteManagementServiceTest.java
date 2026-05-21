package org.bytewright.bgmo.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.bytewright.bgmo.domain.service.data.AdapterSettingsDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SiteManagementServiceTest {
  @Mock private AdapterSettingsDao adapterSettingsDao;
  @InjectMocks private SiteManagementService service;

  @Test
  void testSettings() throws Exception {
    // ARRANGE
    // ACT
    String defaultSettings = service.getDefaultSettings();
    // ASSERT
    assertThat(defaultSettings).isNotBlank();
    assertThat(service.isValidSettingsJson(defaultSettings))
        .isSameAs(AdapterSettingsProvider.ValidationResult.VALID);
  }
}

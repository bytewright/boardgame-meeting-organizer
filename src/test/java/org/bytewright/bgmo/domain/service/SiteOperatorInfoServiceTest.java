package org.bytewright.bgmo.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import java.util.Set;
import org.bytewright.bgmo.domain.model.AdapterSettings;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.service.data.AdapterSettingsDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class SiteOperatorInfoServiceTest {
  private final JsonMapper mapper = JsonMapperFactory.unRedactedMapper();
  @Mock private AdapterSettingsDao adapterSettingsDao;

  @Test
  void testSettings() {
    // ARRANGE
    SiteOperatorInfoService testee = new SiteOperatorInfoService(Set.of(), adapterSettingsDao);

    // ACT
    AdapterSettingsProvider.ValidationResult validationResult =
        testee.isValidSettingsJson(testJson());
    // ASSERT
    assertThat(validationResult).isSameAs(AdapterSettingsProvider.ValidationResult.INVALID);
    // ARRANGE
    AdapterSettings settingsMock = mock();
    Mockito.when(settingsMock.getAdapterSettings()).thenReturn(testJson());
    Mockito.when(adapterSettingsDao.findByAdapter(testee.getAdapterInfo()))
        .thenReturn(settingsMock);
    // ACT
    Optional<ContactInfo.EmailContact> operatorEmail = testee.getOperatorEmail();
    // ASSERT
    assertThat(operatorEmail).isEmpty();

    // ARRANGE
    SiteOperatorInfoService.Settings settings =
        SiteOperatorInfoService.Settings.builder()
            .email(ContactInfo.EmailContact.builder().email("some@mail.org").build())
            .build();
    String json = mapper.writeValueAsString(settings);
    Mockito.when(settingsMock.getAdapterSettings()).thenReturn(json);
    // ACT
    operatorEmail = testee.getOperatorEmail();
    // ASSERT
    assertThat(operatorEmail.orElseThrow().email()).isEqualTo("some@mail.org");
  }

  private String testJson() {
    return """
        {
          "aboutSiteParagraphs" : [ ],
          "address" : null,
          "email" :  "some@mail.org",
          "phone" : null,
          "tosParagraphs" : [ ]
        }
        """;
  }
}

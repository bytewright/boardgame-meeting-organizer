package org.bytewright.bgmo.domain.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.AdapterSettings;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.service.data.AdapterSettingsDao;
import org.bytewright.bgmo.domain.service.notification.ChatBotNotificationTaskExecutor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteOperatorInfoService implements AdapterSettingsProvider {
  private final Set<ChatBotNotificationTaskExecutor> chatBots;
  private final AdapterSettingsDao adapterSettingsDao;
  private final JsonMapper mapper;

  public ContactInfo.AddressContact getOperatorAddress() {
    return getSettings().getAddress();
  }

  public Optional<ContactInfo.PhoneContact> getOperatorPhone() {
    return Optional.ofNullable(getSettings().getPhone());
  }

  public Optional<ContactInfo.EmailContact> getOperatorEmail() {
    return Optional.ofNullable(getSettings().getEmail());
  }

  public boolean isTelegramIntegrationActive() {
    return chatBots.stream()
        .filter(ChatBotNotificationTaskExecutor::isEnabled)
        .anyMatch(bot -> bot.isContactHandlerFor(ContactInfoType.TELEGRAM));
  }

  public boolean isSignalIntegrationActive() {
    return chatBots.stream()
        .filter(ChatBotNotificationTaskExecutor::isEnabled)
        .anyMatch(bot -> bot.isContactHandlerFor(ContactInfoType.SIGNAL));
  }

  public List<String> getTosText() {
    return getSettings().getTosParagraphs();
  }

  private Settings getSettings() {
    AdapterSettings adapterSettings = adapterSettingsDao.findByAdapterName(getAdapterName());
    return mapper.readValue(adapterSettings.getAdapterSettings(), Settings.class);
  }

  @Override
  public String getAdapterName() {
    return "SiteOperatorInfoService";
  }

  @Override
  public boolean isValidSettingsJson(String jsonData) {
    try {
      Settings settings = mapper.readValue(jsonData, Settings.class);
      return settings != null
          && settings.getAddress() != null
          && settings.getTosParagraphs() != null
          && !settings.getTosParagraphs().isEmpty();
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public String getDefaultSettings() throws Exception {
    Settings defaultSettings = Settings.builder().build();
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(defaultSettings);
  }

  @Data
  @Builder
  @Jacksonized
  private static class Settings {
    @Builder.Default
    private ContactInfo.AddressContact address =
        ContactInfo.AddressContact.builder()
            .nameOnBell("Musterman")
            .street("EinStraße")
            .city("Berlin")
            .zipCode("12167")
            .comment("Sample contact...")
            .build();

    @Builder.Default private ContactInfo.PhoneContact phone = null;
    @Builder.Default private ContactInfo.EmailContact email = null;

    @Builder.Default private List<String> tosParagraphs = defaultTos();

    private static List<String> defaultTos() {
      return List.of(
          """
          Diese Plattform ist ein privates, nicht-kommerzielles Angebot.
          Die Nutzung erfolgt auf eigene Verantwortung.
          """,
          """
          Der Betreiber behält sich vor, Nutzerkonten oder Inhalte
          ohne Angabe von Gründen zu sperren oder zu löschen.
          """,
          """
          Es besteht kein Anspruch auf dauerhafte Verfügbarkeit
          des Dienstes. Der Betreiber kann das Angebot jederzeit
          einstellen oder ändern.
          """,
          """
          Durch die Registrierung bestätigen Nutzer, dass sie
          keine falschen Angaben zu ihrer Person machen und
          keine Konten für andere Personen anlegen.
          """);
    }
  }
}

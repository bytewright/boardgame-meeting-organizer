package org.bytewright.bgmo.domain.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.service.notification.ChatBotNotificationTaskExecutor;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteOperatorInfoService {
  private final Set<ChatBotNotificationTaskExecutor> chatBots;

  public ContactInfo.AddressContact getOperatorAddress() {
    return ContactInfo.AddressContact.builder()
        .nameOnBell("Musterman")
        .street("EinStraße")
        .city("Berlin")
        .zipCode("12167")
        .comment("Sample contact...")
        .build();
  }

  public Optional<ContactInfo.PhoneContact> getOperatorPhone() {
    return Optional.empty();
  }

  public Optional<ContactInfo.EmailContact> getOperatorEmail() {
    return Optional.empty();
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

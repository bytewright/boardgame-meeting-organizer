package org.bytewright.bgmo.domain.service.notification;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.usecases.NotificationWorkflows;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeService {
  private final Set<ChatBotNotificationTaskExecutor> chatBots = new HashSet<>();
  private final Map<String, UUID> pendingCodes = new ConcurrentHashMap<>();
  private final NotificationWorkflows notificationWorkflows;

  /** Circular deps workaround */
  void registerChatBots(Set<ChatBotNotificationTaskExecutor> chatBots) {
    this.chatBots.addAll(chatBots);
  }

  public String generateCode(UUID userId) {
    String code = APP_NAME_SHORT + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
    pendingCodes.put(code, userId);
    return code;
  }

  public boolean attemptVerification(String code, ContactInfoType type, String chatId) {
    UUID userId = pendingCodes.remove(code);
    if (userId != null) {
      log.info(
          "Received a correct verification token from user {} for contact type {}", userId, type);
      UUID contactId = notificationWorkflows.verifyContactInfo(userId, type, chatId);
      log.info("Finished verification for {} contact with id: {}", type, contactId);
      return true;
    }
    log.info("Received unknown verification code for type {}: {}", type, code);
    return false;
  }

  public Map<ContactInfoType, String> getBotHandles() {
    return Arrays.stream(ContactInfoType.values())
        .filter(type -> chatBots.stream().anyMatch(bot -> bot.isContactHandlerFor(type)))
        .map(
            type ->
                Map.entry(
                    type,
                    chatBots.stream()
                        .filter(bot -> bot.isContactHandlerFor(type))
                        .findAny()
                        .map(ChatBotNotificationTaskExecutor::botChatDisplayName)
                        .orElseThrow()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}

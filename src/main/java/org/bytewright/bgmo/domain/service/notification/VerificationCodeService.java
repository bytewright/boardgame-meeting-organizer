package org.bytewright.bgmo.domain.service.notification;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.notification.MessengerLinkContext;
import org.bytewright.bgmo.domain.model.notification.VerificationAttempt;
import org.bytewright.bgmo.domain.model.notification.VerificationStep;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.model.user.ContactOption;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.usecases.NotificationWorkflows;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeService {
  private final Set<ChatBotNotificationTaskExecutor> chatBots = new HashSet<>();
  private final Map<String, UUID> pendingCodes = new ConcurrentHashMap<>();
  private final NotificationWorkflows notificationWorkflows;
  private final RegisteredUserDao userDao;

  /** Circular deps workaround */
  void registerChatBots(Set<ChatBotNotificationTaskExecutor> chatBots) {
    this.chatBots.addAll(chatBots);
  }

  public String generateCode(UUID userId) {
    String code = APP_NAME_SHORT + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
    pendingCodes.put(code, userId);
    return code;
  }

  public VerificationAttempt attemptMessengerVerification(
      String code, ContactInfoType type, String chatId, String userName) {
    UUID userId = pendingCodes.remove(code);
    if (userId != null) {
      log.info("Received a correct verification token from user {}, contact type {}", userId, type);
      ContactOption contactOption =
          notificationWorkflows.verifyMessengerContact(userId, type, chatId, userName);
      log.info("Finished verification for {} contact with id: {}", type, contactOption.id());
      RegisteredUser user = userDao.findOrThrow(userId);
      return new VerificationAttempt.Success(user, contactOption);
    }
    log.info("Received unknown verification code for type {}: {}", type, code);
    return new VerificationAttempt.Failed();
  }

  public MessengerLinkContext buildLinkContext(UUID currentUserId, ContactInfoType type) {
    Optional<ChatBotNotificationTaskExecutor> botAdapter =
        chatBots.stream().filter(bot -> bot.isContactHandlerFor(type)).findAny();
    String botDisplayName =
        botAdapter.map(ChatBotNotificationTaskExecutor::botChatDisplayName).orElse("N/A");
    Optional<String> deeplink =
        botAdapter.flatMap(ChatBotNotificationTaskExecutor::generateBotDeepLink);
    List<VerificationStep> verificationSteps =
        botAdapter
            .map(ChatBotNotificationTaskExecutor::generateVerificationSteps)
            .orElse(List.of());
    return new MessengerLinkContext(
        type, generateCode(currentUserId), botDisplayName, deeplink, verificationSteps);
  }
}

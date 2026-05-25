package org.bytewright.bgmo.domain.service.notification;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import jakarta.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.notification.LinkingAttempt;
import org.bytewright.bgmo.domain.model.notification.MessengerLinkContext;
import org.bytewright.bgmo.domain.model.notification.NotificationChannel;
import org.bytewright.bgmo.domain.model.notification.VerificationStep;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.model.user.ContactOption;
import org.bytewright.bgmo.domain.service.data.ModelDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.usecases.NotificationWorkflows;
import org.bytewright.bgmo.usecases.UserWorkflows;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationLinkCodeService {
  private final Set<ChatBotNotificationTaskExecutor> chatBots = new HashSet<>();
  private final Map<String, UUID> pendingCodes = new ConcurrentHashMap<>();
  private final NotificationWorkflows notificationWorkflows;
  private final ModelDao<ContactOption> contactOptionDao;
  private final UserWorkflows userWorkflows;
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

  public LinkingAttempt attemptMessengerLinking(
      String code,
      ContactInfoType type,
      NotificationChannel channel,
      @Nullable ContactInfo contactInfo) {
    UUID userId = pendingCodes.remove(code);
    if (userId != null) {
      log.info("Received a correct linking token from user {}, contact type {}", userId, type);
      notificationWorkflows.linkNotificationChannel(userId, channel);
      if (contactInfo != null) {
        UUID contactOptionId = userWorkflows.addContactInfo(userId, contactInfo, true);
        return new LinkingAttempt.LinkAndContactOption(userId, contactOptionId);
      }
      return new LinkingAttempt.Success(userId);
    }
    log.info("Received unknown verification code for type {}: {}", type, code);
    return new LinkingAttempt.Failed();
  }

  public MessengerLinkContext buildLinkContext(UUID currentUserId, ContactInfoType type) {
    Optional<ChatBotNotificationTaskExecutor> botAdapter =
        chatBots.stream().filter(bot -> bot.isContactHandlerFor(type)).findAny();
    String botDisplayName =
        botAdapter.map(ChatBotNotificationTaskExecutor::botChatDisplayName).orElse("N/A");
    Optional<String> deeplink =
        botAdapter.flatMap(ChatBotNotificationTaskExecutor::generateBotDeepLink);
    List<VerificationStep> linkingSteps =
        botAdapter.map(ChatBotNotificationTaskExecutor::generateLinkingSteps).orElse(List.of());
    return new MessengerLinkContext(
        type, generateCode(currentUserId), botDisplayName, deeplink, linkingSteps);
  }
}

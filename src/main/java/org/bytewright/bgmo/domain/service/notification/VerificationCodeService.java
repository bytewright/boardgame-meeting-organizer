package org.bytewright.bgmo.domain.service.notification;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.usecases.NotificationWorkflows;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeService {
  private final Map<String, UUID> pendingCodes = new ConcurrentHashMap<>();
  private final NotificationWorkflows notificationWorkflows;

  public String generateCode(UUID userId) {
    String code = APP_NAME_SHORT + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
    pendingCodes.put(code, userId);
    return code;
  }

  public boolean attemptVerification(String code, ContactInfoType type, String chatId) {
    UUID userId = pendingCodes.remove(code);
    if (userId != null) {
      log.info("Received a correct verification token from user {}", userId);
      UUID contactId = notificationWorkflows.verifyContactInfo(userId, type, chatId);
      log.info("Finished verification for contact with id: {}", contactId);
      return true;
    }
    log.info("Received unknown verification code for type {}: {}", type, code);
    return false;
  }
}

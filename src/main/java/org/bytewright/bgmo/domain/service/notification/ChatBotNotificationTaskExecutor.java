package org.bytewright.bgmo.domain.service.notification;

import java.util.List;
import java.util.Optional;
import org.bytewright.bgmo.domain.model.notification.VerificationStep;

public interface ChatBotNotificationTaskExecutor extends NotificationTaskExecutor {
  boolean isEnabled();

  String botChatDisplayName();

  Optional<String> generateBotDeepLink();

  List<VerificationStep> generateVerificationSteps();
}

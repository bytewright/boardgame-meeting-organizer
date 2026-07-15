package org.bytewright.bgmo.domain.service.notification;

import java.util.List;
import java.util.Optional;
import org.bytewright.bgmo.domain.model.notification.VerificationStep;

public interface ChatBotNotificationTaskExecutor extends NotificationTaskExecutor {
  boolean isEnabled();

  String botChatDisplayName();

  /**
   * Only used for displaying tutorial steps how to link external supported service with app, i.e.
   * "contact bot at [deeplink]"
   */
  Optional<String> generateBotDeepLink();

  List<VerificationStep> generateLinkingSteps();
}

package org.bytewright.bgmo.adapter.notification.signal;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.bytewright.bgmo.domain.model.notification.NotificationContext;
import org.bytewright.bgmo.domain.model.notification.VerificationStep;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.service.notification.ChatBotNotificationTaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignalNotificationAdapter implements ChatBotNotificationTaskExecutor {
  private static final String ADAPTER_NAME = "Signal-ChatBotNotificationTaskExecutor-integration";

  @Override
  public boolean supports(NotificationContext context) {
    return false;
  }

  @Override
  public boolean isContactHandlerFor(ContactInfoType type) {
    return type == ContactInfoType.SIGNAL;
  }

  @Override
  @Async
  public void execute(NotificationContext context) {
    throw new NotImplementedException("Signal integration is work-in-progress");
  }

  @Override
  public boolean isEnabled() {
    return false;
  }

  @Override
  public String botChatDisplayName() {
    return "Signal ist noch in arbeit";
  }

  @Override
  public Optional<String> generateBotDeepLink() {
    return Optional.empty();
  }

  @Override
  public List<VerificationStep> generateLinkingSteps() {
    return List.of(VerificationStep.builder().messageKey("adapter.signal.tutorial.step1").build());
  }
}

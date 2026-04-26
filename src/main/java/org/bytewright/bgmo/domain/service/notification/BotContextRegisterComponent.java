package org.bytewright.bgmo.domain.service.notification;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BotContextRegisterComponent implements InitializingBean {
  private final VerificationCodeService verificationCodeService;
  private final Set<ChatBotNotificationTaskExecutor> chatBots;

  @Override
  public void afterPropertiesSet() throws Exception {
    verificationCodeService.registerChatBots(chatBots);
  }
}

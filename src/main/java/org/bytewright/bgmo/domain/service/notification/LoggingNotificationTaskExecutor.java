package org.bytewright.bgmo.domain.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.notification.NotificationContext;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoggingNotificationTaskExecutor implements NotificationTaskExecutor {
  @Override
  public boolean supports(NotificationContext context) {
    return true;
  }

  @Override
  public boolean isContactHandlerFor(ContactInfoType type) {
    return false;
  }

  @Override
  public void execute(NotificationContext context) {
    log.info("Message dispatched: {}", context);
  }
}

package org.bytewright.bgmo.domain.service.notification;

import org.bytewright.bgmo.domain.model.notification.NotificationContext;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;

public interface NotificationTaskExecutor {
  boolean supports(NotificationContext context);

  boolean isContactHandlerFor(ContactInfoType type);

  void execute(NotificationContext context);
}

package org.bytewright.bgmo.domain.service.notification;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationTaskExecutorRegisterComponent implements InitializingBean {
  private final Set<NotificationTaskExecutor> executors;
  private final NotificationManager notificationManager;

  @Override
  public void afterPropertiesSet() throws Exception {
    log.info("Registering {} NotificationTaskExecutor with service", executors.size());
    notificationManager.registerTaskExecutors(executors);
  }
}

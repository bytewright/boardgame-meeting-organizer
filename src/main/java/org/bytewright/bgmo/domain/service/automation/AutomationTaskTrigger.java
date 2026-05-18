package org.bytewright.bgmo.domain.service.automation;

import jakarta.transaction.Transactional;
import java.time.ZonedDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.automation.ScheduledTask;
import org.bytewright.bgmo.domain.service.data.AutomationTaskDao;
import org.bytewright.bgmo.usecases.AutomationTaskWorkflows;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationTaskTrigger {
  private static final int TASK_BATCH_SIZE = 100;
  private final AutomationTaskWorkflows automationTaskWorkflows;
  private final AutomationTaskDao automationTaskDao;
  private final ExecutorService executor = Executors.newFixedThreadPool(3);
  private final TimeSource timeSource;

  @Scheduled(fixedDelay = 10, initialDelay = 45, timeUnit = TimeUnit.SECONDS)
  public void periodicTaskExecTrigger() {
    ZonedDateTime now = timeSource.nowZDT();
    var tasks = automationTaskDao.findTasksToComplete(TASK_BATCH_SIZE, now.toInstant());
    tasks.forEach(task -> executor.submit(() -> automationTaskWorkflows.executeTask(task.getId())));
  }

  @Transactional
  @Scheduled(fixedDelay = 1, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
  public void periodicCleanupStuckTasks() {
    ZonedDateTime now = timeSource.nowZDT();
    for (ScheduledTask task : automationTaskDao.findExecutingTasks()) {
      ZonedDateTime stuckDeadline =
          task.getTsModified().plus(task.getStuckTimeout()).atZone(now.getOffset());
      if (now.isAfter(stuckDeadline)) {
        log.warn("Detected stuck task {}, setting to error state", task.id());
        automationTaskDao.markError(task.id());
      }
    }
  }
}

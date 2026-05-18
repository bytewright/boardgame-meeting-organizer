package org.bytewright.bgmo.domain.service.automation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.bytewright.bgmo.domain.model.automation.ScheduledTask;
import org.bytewright.bgmo.domain.model.automation.TaskPayload;
import org.bytewright.bgmo.domain.service.data.AutomationTaskDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class AutomationTaskTriggerIntegrationTest {

  @Autowired private AutomationTaskTrigger trigger;
  @Autowired private AutomationTaskDao automationTaskDao;
  @Autowired private AppTimeSource timeSource;
  @Autowired private TransactionTemplate transactionTemplate;

  @Test
  void shouldClaimExecuteAndFinishTask() {
    // ==================== ARRANGE ====================
    Instant now = timeSource.now();
    ZonedDateTime dueDate = ZonedDateTime.now().minusSeconds(10);

    ScheduledTask task =
        ScheduledTask.builder()
            .tsCreation(now)
            .tsModified(now)
            .tsDueDate(dueDate)
            .stuckTimeout(Duration.ofMinutes(5))
            .taskState(ScheduledTask.TaskState.PENDING)
            .payload(new TaskPayload.MeetupCleanup(UUID.randomUUID()))
            .build();
    UUID taskId =
        transactionTemplate.execute(status -> automationTaskDao.createOrUpdate(task).getId());

    // ==================== ACT ====================
    trigger.periodicTaskExecTrigger();

    // ==================== ASSERT ====================
    await()
        .atMost(Duration.ofSeconds(5))
        .pollInterval(Duration.ofMillis(200))
        .untilAsserted(
            () -> {
              ScheduledTask processed =
                  transactionTemplate.execute(
                      status ->
                          automationTaskDao
                              .findAllTasks(
                                  0,
                                  10,
                                  AutomationTaskDao.TaskSorting.TASK_STATE,
                                  Sort.Direction.DESC)
                              .content()
                              .stream()
                              .filter(t -> t.getId().equals(taskId))
                              .findFirst()
                              .orElseThrow());

              assertThat(processed)
                  .isNotNull()
                  .extracting(ScheduledTask::getTaskState)
                  .isSameAs(ScheduledTask.TaskState.FINISHED);
            });
  }
}

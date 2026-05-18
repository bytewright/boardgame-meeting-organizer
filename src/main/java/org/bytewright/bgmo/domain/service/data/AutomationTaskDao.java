package org.bytewright.bgmo.domain.service.data;

import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.bytewright.bgmo.domain.model.automation.ScheduledTask;
import org.springframework.data.domain.Sort;

public interface AutomationTaskDao extends ModelDao<ScheduledTask> {
  ScheduledTaskPage findAllTasks(
      int pageNumber, int pageSize, TaskSorting sorting, Sort.Direction direction);

  @Transactional(Transactional.TxType.REQUIRES_NEW)
  List<ScheduledTask> findTasksToComplete(int limit, Instant now);

  void markError(UUID taskId);

  void markFinished(UUID taskId);

  List<ScheduledTask> findExecutingTasks();

  enum TaskSorting {
    DUE_DATE,
    TASK_STATE,
  }

  record ScheduledTaskPage(
      long totalResultCount,
      int currentPage,
      int pageSize,
      TaskSorting sorting,
      Sort.Direction asc,
      List<ScheduledTask> content) {
    public boolean hasNextPage() {
      int lastElementIndex = currentPage * pageSize;
      return lastElementIndex < totalResultCount;
    }
  }
}

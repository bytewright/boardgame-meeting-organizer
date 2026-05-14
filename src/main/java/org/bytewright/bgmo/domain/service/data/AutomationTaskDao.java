package org.bytewright.bgmo.domain.service.data;

import java.util.List;
import org.bytewright.bgmo.domain.model.automation.ScheduledTask;
import org.springframework.data.domain.Sort;

public interface AutomationTaskDao extends ModelDao<ScheduledTask> {
  ScheduledTaskPage findAllTasks(
      int pageNumber, int pageSize, TaskSorting sorting, Sort.Direction direction);

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
      List<ScheduledTask> content) {}
}

package org.bytewright.bgmo.adapter.persistence.dao.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.bytewright.bgmo.adapter.persistence.entity.ScheduledTaskEntity;
import org.bytewright.bgmo.domain.model.automation.ScheduledTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduledTaskRepository
    extends JpaRepository<ScheduledTaskEntity, UUID>,
        JpaSpecificationExecutor<ScheduledTaskEntity> {
  @Query(
      value =
          """
              SELECT *
              FROM scheduled_tasks
              WHERE task_state = 'PENDING'
                AND ts_due_date <= :timestamp
              ORDER BY ts_due_date
              LIMIT :limit
              FOR UPDATE SKIP LOCKED
              """,
      nativeQuery = true)
  List<ScheduledTaskEntity> claimableTasks(int limit, Instant timestamp);

  Stream<ScheduledTaskEntity> findByTaskState(ScheduledTask.TaskState taskState);
}

package org.bytewright.bgmo.adapter.persistence.dao.mapstruct;

import static org.bytewright.bgmo.domain.model.automation.ScheduledTask.TaskState.*;

import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.persistence.dao.BaseEntityMapper;
import org.bytewright.bgmo.adapter.persistence.dao.BaseMapperConfig;
import org.bytewright.bgmo.adapter.persistence.dao.repository.ScheduledTaskRepository;
import org.bytewright.bgmo.adapter.persistence.entity.ScheduledTaskEntity;
import org.bytewright.bgmo.adapter.persistence.entity.ScheduledTaskEntity_;
import org.bytewright.bgmo.domain.model.automation.ScheduledTask;
import org.bytewright.bgmo.domain.model.automation.TaskPayload;
import org.bytewright.bgmo.domain.service.data.AutomationTaskDao;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Transactional
@Mapper(config = BaseMapperConfig.class)
@Setter(onMethod_ = {@Autowired})
public abstract class ScheduledTaskEntityMapper
    extends BaseEntityMapper<ScheduledTask, ScheduledTaskEntity> implements AutomationTaskDao {
  private ScheduledTaskRepository taskRepository;
  private JsonMapper objectMapper;

  @Mapping(target = "idempotencyKey", source = "payload.idempotencyKey")
  @Override
  public abstract void updateEntity(
      @MappingTarget ScheduledTaskEntity currentEntity, ScheduledTask model);

  @BeanMapping(ignoreUnmappedSourceProperties = {"idempotencyKey"})
  @Override
  public abstract ScheduledTask toDto(ScheduledTaskEntity entity);

  @Override
  protected Class<ScheduledTaskEntity> getEntityClass() {
    return ScheduledTaskEntity.class;
  }

  protected String serializeTaskPayload(TaskPayload value) {
    return objectMapper.writeValueAsString(value);
  }

  protected TaskPayload deserializePayload(String payload) {
    return objectMapper.readValue(payload, TaskPayload.class);
  }

  @Override
  public ScheduledTaskPage findAllTasks(
      int pageNumber, int pageSize, TaskSorting sorting, Sort.Direction direction) {
    String sortProperty =
        switch (sorting) {
          case DUE_DATE -> ScheduledTaskEntity_.TS_DUE_DATE;
          case TASK_STATE -> ScheduledTaskEntity_.TASK_STATE;
        };

    Sort sort = Sort.by(direction, sortProperty);
    Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
    Page<ScheduledTaskEntity> taskEntityPage = taskRepository.findAll(pageable);
    List<ScheduledTask> dtos = taskEntityPage.stream().map(this::toDto).toList();
    return new ScheduledTaskPage(
        taskEntityPage.getTotalElements(), pageNumber, pageSize, sorting, direction, dtos);
  }

  @Override
  public List<ScheduledTask> findTasksToComplete(int limit, Instant now) {
    List<ScheduledTaskEntity> claimabledTasks = taskRepository.claimableTasks(limit, now);
    for (ScheduledTaskEntity task : claimabledTasks) {
      task.setTaskState(EXECUTING);
      taskRepository.save(task);
    }
    return claimabledTasks.stream().map(this::toDto).toList();
  }

  @Override
  public void markError(UUID taskId) {
    ScheduledTaskEntity scheduledTaskEntity = taskRepository.findById(taskId).orElseThrow();
    scheduledTaskEntity.setTaskState(ERROR);
    taskRepository.save(scheduledTaskEntity);
  }

  @Override
  public void markFinished(UUID taskId) {
    ScheduledTaskEntity scheduledTaskEntity = taskRepository.findById(taskId).orElseThrow();
    scheduledTaskEntity.setTaskState(FINISHED);
    taskRepository.save(scheduledTaskEntity);
  }

  @Override
  public List<ScheduledTask> findExecutingTasks() {
    return taskRepository.findByTaskState(EXECUTING).map(this::toDto).toList();
  }
}

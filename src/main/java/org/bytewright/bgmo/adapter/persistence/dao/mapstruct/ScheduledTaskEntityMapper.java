package org.bytewright.bgmo.adapter.persistence.dao.mapstruct;

import jakarta.transaction.Transactional;
import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.persistence.dao.BaseEntityMapper;
import org.bytewright.bgmo.adapter.persistence.dao.BaseMapperConfig;
import org.bytewright.bgmo.adapter.persistence.dao.repository.ScheduledTaskRepository;
import org.bytewright.bgmo.adapter.persistence.entity.ScheduledTaskEntity;
import org.bytewright.bgmo.adapter.persistence.entity.ScheduledTaskEntity_;
import org.bytewright.bgmo.domain.model.automation.ScheduledTask;
import org.bytewright.bgmo.domain.model.automation.ScheduledTaskPayload;
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

  protected String serializeTaskPayload(ScheduledTaskPayload value) {
    return objectMapper.writeValueAsString(value);
  }

  protected ScheduledTaskPayload deserializePayload(String payload) {
    return objectMapper.readValue(payload, ScheduledTaskPayload.class);
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
}

package org.bytewright.bgmo.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.*;
import org.bytewright.bgmo.domain.model.automation.ScheduledTask;
import org.bytewright.bgmo.domain.model.data.HasUUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
    name = "scheduled_tasks",
    uniqueConstraints =
        @UniqueConstraint(
            name = "UC_SCHEDULED_TASK_KEY",
            columnNames = {ScheduledTaskEntity_.IDEMPOTENCY_KEY}))
@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ScheduledTaskEntity extends AbstractEntity<UUID> implements HasUUID {

  @Id
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant tsCreation;

  @LastModifiedDate
  @Column(name = "modified_at")
  private Instant tsModified;

  @Column(name = "ts_due_date", nullable = false)
  private ZonedDateTime tsDueDate;

  @Column(name = "stuck_timeout", nullable = false)
  private Duration stuckTimeout;

  @Column(name = "idempotency_key", nullable = false)
  private String idempotencyKey;

  @Column(name = "task_state", nullable = false)
  @Enumerated(EnumType.STRING)
  private ScheduledTask.TaskState taskState;

  @Column(nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String payload;
}

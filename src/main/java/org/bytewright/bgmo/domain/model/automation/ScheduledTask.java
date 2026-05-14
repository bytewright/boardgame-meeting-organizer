package org.bytewright.bgmo.domain.model.automation;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import org.bytewright.bgmo.domain.model.data.HasUUID;

@Data
@Builder
public class ScheduledTask implements HasUUID {
  private UUID id;
  private Instant tsCreation;
  private Instant tsModified;
  private ZonedDateTime tsDueDate;
  private Duration stuckTimeout;
  private TaskState taskState;
  private ScheduledTaskPayload payload;

  public enum TaskState {
    PENDING,
    EXECUTING,
    FINISHED,
    ERROR
  }
}

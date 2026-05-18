package org.bytewright.bgmo.domain.model.automation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.UUID;
import lombok.Builder;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(
      value = TaskPayload.MeetupSlotDistribution.class,
      name = "MEETUP_SLOT_DISTRIBUTION"),
  @JsonSubTypes.Type(value = TaskPayload.MeetupCleanup.class, name = "MEETUP_CLEANUP"),
  @JsonSubTypes.Type(
      value = TaskPayload.MeetupUpcomingNotification.class,
      name = "MEETUP_UPCOMING_NOTIFICATION"),
  @JsonSubTypes.Type(value = TaskPayload.LiftUserSuspension.class, name = "LIFT_SUSPENSION"),
  @JsonSubTypes.Type(value = TaskPayload.AdapterTask.class, name = "ADAPTER")
})
public sealed interface TaskPayload
    permits TaskPayload.MeetupSlotDistribution,
        TaskPayload.MeetupCleanup,
        TaskPayload.MeetupUpcomingNotification,
        TaskPayload.LiftUserSuspension,
        TaskPayload.AdapterTask {

  String discriminator(); // stable string for DB

  String getIdempotencyKey();

  @Builder
  record MeetupSlotDistribution(UUID meetupId) implements TaskPayload {

    public String discriminator() {
      return "MEETUP_SLOT_DISTRIBUTION";
    }

    @Override
    public String getIdempotencyKey() {
      return "%s-%s".formatted(discriminator(), meetupId);
    }
  }

  @Builder
  record MeetupCleanup(UUID meetupId) implements TaskPayload {

    public String discriminator() {
      return "MEETUP_CLEANUP";
    }

    @Override
    public String getIdempotencyKey() {
      return "%s-%s".formatted(discriminator(), meetupId);
    }
  }

  @Builder
  record MeetupUpcomingNotification(UUID meetupId) implements TaskPayload {

    public String discriminator() {
      return "MEETUP_UPCOMING_NOTIFICATION";
    }

    @Override
    public String getIdempotencyKey() {
      return "%s-%s".formatted(discriminator(), meetupId);
    }
  }

  @Builder
  record LiftUserSuspension(UUID userId) implements TaskPayload {

    public String discriminator() {
      return "LIFT_SUSPENSION";
    }

    @Override
    public String getIdempotencyKey() {
      return "%s-%s".formatted(discriminator(), userId);
    }
  }

  @Builder
  // Escape hatch for adapter tasks that shouldn't need core knowledge
  record AdapterTask(String adapterName, String taskKey, String innerPayload)
      implements TaskPayload {

    public String discriminator() {
      return "ADAPTER_" + adapterName;
    }

    @Override
    public String getIdempotencyKey() {
      return "%s-%s".formatted(discriminator(), taskKey);
    }
  }
}

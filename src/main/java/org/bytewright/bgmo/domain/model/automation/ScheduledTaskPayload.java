package org.bytewright.bgmo.domain.model.automation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.UUID;
import lombok.Builder;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(
      value = ScheduledTaskPayload.MeetupSlotDistributionPayload.class,
      name = "MEETUP_SLOT_DISTRIBUTION"),
  @JsonSubTypes.Type(
      value = ScheduledTaskPayload.MeetupCleanupPayload.class,
      name = "MEETUP_CLEANUP"),
  @JsonSubTypes.Type(
      value = ScheduledTaskPayload.MeetupUpcomingNotificationPayload.class,
      name = "MEETUP_UPCOMING_NOTIFICATION"),
  @JsonSubTypes.Type(
      value = ScheduledTaskPayload.LiftUserSuspensionPayload.class,
      name = "LIFT_SUSPENSION"),
  @JsonSubTypes.Type(value = ScheduledTaskPayload.AdapterTaskPayload.class, name = "ADAPTER")
})
public sealed interface ScheduledTaskPayload
    permits ScheduledTaskPayload.MeetupSlotDistributionPayload,
        ScheduledTaskPayload.MeetupCleanupPayload,
        ScheduledTaskPayload.MeetupUpcomingNotificationPayload,
        ScheduledTaskPayload.LiftUserSuspensionPayload,
        ScheduledTaskPayload.AdapterTaskPayload {

  String discriminator(); // stable string for DB

  String getIdempotencyKey();

  @Builder
  record MeetupSlotDistributionPayload(UUID meetupId) implements ScheduledTaskPayload {

    public String discriminator() {
      return "MEETUP_SLOT_DISTRIBUTION";
    }

    @Override
    public String getIdempotencyKey() {
      return "%s-%s".formatted(discriminator(), meetupId);
    }
  }

  @Builder
  record MeetupCleanupPayload(UUID meetupId) implements ScheduledTaskPayload {

    public String discriminator() {
      return "MEETUP_CLEANUP";
    }

    @Override
    public String getIdempotencyKey() {
      return "%s-%s".formatted(discriminator(), meetupId);
    }
  }

  @Builder
  record MeetupUpcomingNotificationPayload(UUID meetupId) implements ScheduledTaskPayload {

    public String discriminator() {
      return "MEETUP_UPCOMING_NOTIFICATION";
    }

    @Override
    public String getIdempotencyKey() {
      return "%s-%s".formatted(discriminator(), meetupId);
    }
  }

  @Builder
  record LiftUserSuspensionPayload(UUID userId) implements ScheduledTaskPayload {

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
  record AdapterTaskPayload(String adapterName, String taskKey, String innerPayload)
      implements ScheduledTaskPayload {

    public String discriminator() {
      return "ADAPTER_" + adapterName;
    }

    @Override
    public String getIdempotencyKey() {
      return "%s-%s".formatted(discriminator(), taskKey);
    }
  }
}

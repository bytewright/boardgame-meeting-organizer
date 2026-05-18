package org.bytewright.bgmo.usecases;

import jakarta.transaction.Transactional;
import java.time.Duration;
import java.util.UUID;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.AdapterSettings;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.automation.ScheduledTask;
import org.bytewright.bgmo.domain.model.automation.TaskPayload;
import org.bytewright.bgmo.domain.model.event.ModelUpdatedEvents;
import org.bytewright.bgmo.domain.service.AdapterSettingsProvider;
import org.bytewright.bgmo.domain.service.JsonMapperFactory;
import org.bytewright.bgmo.domain.service.data.AdapterSettingsDao;
import org.bytewright.bgmo.domain.service.data.AutomationTaskDao;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AutomationTaskWorkflows implements AdapterSettingsProvider {
  private final SlotDistributionWorkflows slotDistributionWorkflows;
  private final NotificationWorkflows notificationWorkflows;
  private final AdapterSettingsDao adapterSettingsDao;
  private final MeetupWorkflows meetupWorkflows;
  private final UserWorkflows userWorkflows;
  private final AutomationTaskDao automationTaskDao;
  private final MeetupDao meetupDao;
  private final JsonMapper mapper = JsonMapperFactory.unRedactedMapper();

  @EventListener
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void onMeetupCreatedEvent(ModelUpdatedEvents.MeetupCreated event) {
    MeetupEvent meetupEvent = meetupDao.findOrThrow(event.id());
    AutomationSettings settings = getSettings();
    automationTaskDao.createOrUpdate(
        ScheduledTask.builder()
            .tsDueDate(meetupEvent.getRegistrationClosing())
            .stuckTimeout(Duration.ofMinutes(1))
            .taskState(ScheduledTask.TaskState.PENDING)
            .payload(
                TaskPayload.MeetupSlotDistribution.builder().meetupId(meetupEvent.id()).build())
            .build());
    automationTaskDao.createOrUpdate(
        ScheduledTask.builder()
            .tsDueDate(meetupEvent.getEventDate().minusHours(settings.eventNotificationHours()))
            .stuckTimeout(Duration.ofMinutes(1))
            .taskState(ScheduledTask.TaskState.PENDING)
            .payload(
                TaskPayload.MeetupUpcomingNotification.builder().meetupId(meetupEvent.id()).build())
            .build());
    automationTaskDao.createOrUpdate(
        ScheduledTask.builder()
            .tsDueDate(meetupEvent.getEventDate().plusDays(settings.eventDeletionDaysAfter()))
            .stuckTimeout(Duration.ofMinutes(1))
            .taskState(ScheduledTask.TaskState.PENDING)
            .payload(TaskPayload.MeetupCleanup.builder().meetupId(meetupEvent.id()).build())
            .build());
  }

  private AutomationSettings getSettings() {
    AdapterSettings adapterSettings = adapterSettingsDao.findByAdapter(getAdapterInfo());
    return mapper.readValue(adapterSettings.getAdapterSettings(), AutomationSettings.class);
  }

  @Override
  public AdapterInfo getAdapterInfo() {
    return AdapterInfo.builder()
        .stableName("core.use-case.automation")
        .description("Default values regarding automation work flows")
        .build();
  }

  @Override
  public ValidationResult isValidSettingsJson(String jsonData) {
    try {
      var settings = mapper.readValue(jsonData, AutomationSettings.class);
      return settings != null ? ValidationResult.VALID : ValidationResult.INVALID;
    } catch (JacksonException e) {
      log.error("Error while validating json: {}", e.getMessage());
    }
    return ValidationResult.INVALID;
  }

  @Override
  public String getDefaultSettings() throws Exception {
    return mapper.writeValueAsString(
        AutomationSettings.builder().eventDeletionDaysAfter(10).eventNotificationHours(3).build());
  }

  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void executeTask(UUID taskId) {
    try {
      ScheduledTask task = automationTaskDao.findOrThrow(taskId);
      log.info(
          "Starting execution of task {}, due at {}, created at {}",
          task.getPayload().getIdempotencyKey(),
          task.getTsDueDate(),
          task.getTsCreation());
      switch (task.getPayload()) {
        case TaskPayload.AdapterTask adapterTask -> {
          /* ignore for now, needs to be delegated to the adapters but no usecase so far */
        }
        case TaskPayload.LiftUserSuspension liftUserSuspension ->
            userWorkflows.liftUserSuspension(liftUserSuspension.userId());
        case TaskPayload.MeetupCleanup meetupCleanup ->
            meetupWorkflows.removeExpiredMeetup(meetupCleanup.meetupId());
        case TaskPayload.MeetupSlotDistribution meetupSlotDistribution ->
            slotDistributionWorkflows.distributeSlots(meetupSlotDistribution.meetupId());
        case TaskPayload.MeetupUpcomingNotification meetupUpcomingNotification ->
            notificationWorkflows.triggerUpcomingMeetingNotifications(
                meetupUpcomingNotification.meetupId());
      }
      log.info(
          "Finished execution of task {}, marking task as finished",
          task.getPayload().getIdempotencyKey());
      automationTaskDao.markFinished(task.getId());
    } catch (Exception e) {
      log.error("Task with id {} failed", taskId, e);
      automationTaskDao.markError(taskId);
    }
  }

  @Builder
  @Jacksonized
  private record AutomationSettings(int eventDeletionDaysAfter, int eventNotificationHours) {}
}

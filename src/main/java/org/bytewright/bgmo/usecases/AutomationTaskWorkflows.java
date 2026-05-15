package org.bytewright.bgmo.usecases;

import jakarta.transaction.Transactional;
import java.time.Duration;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.AdapterSettings;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.automation.ScheduledTask;
import org.bytewright.bgmo.domain.model.automation.ScheduledTaskPayload;
import org.bytewright.bgmo.domain.service.AdapterSettingsProvider;
import org.bytewright.bgmo.domain.service.JsonMapperFactory;
import org.bytewright.bgmo.domain.service.data.AdapterSettingsDao;
import org.bytewright.bgmo.domain.service.data.AutomationTaskDao;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AutomationTaskWorkflows implements AdapterSettingsProvider {
  private final AdapterSettingsDao adapterSettingsDao;
  private final AutomationTaskDao automationTaskDao;
  private final JsonMapper mapper = JsonMapperFactory.unRedactedMapper();

  public void schedule(MeetupEvent meetupEvent) {
    AutomationSettings settings = getSettings();
    automationTaskDao.createOrUpdate(
        ScheduledTask.builder()
            .tsDueDate(meetupEvent.getRegistrationClosing())
            .stuckTimeout(Duration.ofMinutes(1))
            .taskState(ScheduledTask.TaskState.PENDING)
            .payload(
                ScheduledTaskPayload.MeetupSlotDistributionPayload.builder()
                    .meetupId(meetupEvent.id())
                    .build())
            .build());
    automationTaskDao.createOrUpdate(
        ScheduledTask.builder()
            .tsDueDate(meetupEvent.getEventDate().minusHours(settings.eventNotificationHours()))
            .stuckTimeout(Duration.ofMinutes(1))
            .taskState(ScheduledTask.TaskState.PENDING)
            .payload(
                ScheduledTaskPayload.MeetupUpcomingNotificationPayload.builder()
                    .meetupId(meetupEvent.id())
                    .build())
            .build());
    automationTaskDao.createOrUpdate(
        ScheduledTask.builder()
            .tsDueDate(meetupEvent.getEventDate().plusDays(settings.eventDeletionDaysAfter()))
            .stuckTimeout(Duration.ofMinutes(1))
            .taskState(ScheduledTask.TaskState.PENDING)
            .payload(
                ScheduledTaskPayload.MeetupCleanupPayload.builder()
                    .meetupId(meetupEvent.id())
                    .build())
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

  @Builder
  @Jacksonized
  private record AutomationSettings(int eventDeletionDaysAfter, int eventNotificationHours) {}
}

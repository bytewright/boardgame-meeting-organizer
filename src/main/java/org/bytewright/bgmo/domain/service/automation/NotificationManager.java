package org.bytewright.bgmo.domain.service.automation;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bytewright.bgmo.domain.model.notification.NotificationContext;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.bytewright.bgmo.domain.service.notification.NotificationTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * Central hub for pushing notifications about events to user. This will check implemented contact
 * options, user preferred method of contact and then trigger message delivery
 */
@Service
@RequiredArgsConstructor
public class NotificationManager {
  private final List<NotificationTaskExecutor> executors;
  private final MeetupDao meetupDao;

  public void addNewEventCreatedTask(UUID meetupId) {
    var meetup = meetupDao.findById(meetupId).orElseThrow();
    var context =
        NotificationContext.builder()
            .message("New Meetup: *" + meetup.getTitle() + "* 🎲")
            .targetId(
                System.getProperty("bgmo.adapter.bot.telegram.group-chat-id")) // Or via config
            .type(ContactInfoType.TELEGRAM)
            .metadata(Map.of("meetupId", meetupId.toString()))
            .build();

    dispatch(context);
  }

  public void addUserApprovedTask(UUID userId) {
    // todo add async task to notify user via primary channel that the account is active
  }

  public void addNewJoinRequestCreatedTask(UUID meetupId, UUID requestId) {
    // todo add async task to notify event creator that a new request was created
  }

  public void addJoinRequestApprovedTask(UUID meetupId, UUID requestId, UUID userId) {
    // todo add async task to notify user that a request was accepted
  }

  private void dispatch(NotificationContext context) {
    executors.stream().filter(e -> e.supports(context.type())).forEach(e -> e.execute(context));
  }
}

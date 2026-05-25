package org.bytewright.bgmo.usecases;

import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.notification.NotificationChannel;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationWorkflows {
  private final RegisteredUserDao userDao;
  private final MeetupDao meetupDao;

  public void linkNotificationChannel(UUID userId, NotificationChannel channel) {
    RegisteredUser user = userDao.findOrThrow(userId);
    NotificationChannel currentChannel = user.getNotificationChannel();
    if (currentChannel.equals(channel)) {
      log.warn("Tried to set already established notification channel for user {}", userId);
      return;
    }
    log.info(
        "Setting new Notification channel for user {}: {}. Old discarded channel: {}",
        userId,
        channel,
        currentChannel);
    user.setNotificationChannel(channel);
    userDao.createOrUpdate(user);
  }

  public void triggerUpcomingMeetingNotifications(UUID meetupId) {
    meetupDao
        .find(meetupId)
        .ifPresent(
            meetupEvent ->
                log.info(
                    "Should send out infos about upcoming meeting: {}", meetupEvent.logIdentity()));
  }
}

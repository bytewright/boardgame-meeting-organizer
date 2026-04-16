package org.bytewright.bgmo.domain.service.automation;

import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Central hub for pushing notifications about events to user. This will check implemented contact
 * options, user preferred method of contact and then trigger message delivery
 */
@Service
public class NotificationManager {
  public void addUserApprovedTask(UUID userId) {
    // todo add async task to notify user via primary channel that the account is active
  }

  public void addNewEventCreatedTask(UUID meetupId) {
    // todo add async task to notify all users or a channel that a new event was created
  }

  public void addNewJoinRequestCreatedTask(UUID meetupId, UUID requestId) {
    // todo add async task to notify event creator that a new request was created
  }

  public void addJoinRequestApprovedTask(UUID meetupId, UUID requestId, UUID userId) {
    // todo add async task to notify user that a request was accepted
  }
}

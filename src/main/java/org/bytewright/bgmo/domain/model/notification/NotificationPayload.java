package org.bytewright.bgmo.domain.model.notification;

import static org.bytewright.bgmo.domain.model.notification.NotificationPayload.*;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.Builder;

public sealed interface NotificationPayload
    permits JoinRequestApproved,
        JoinRequestCreated,
        MeetupCanceled,
        MeetupCreated,
        MeetupRescheduled,
        UserApproved,
        UserMessengerLinked,
        UserMessengerLinkingFailed,
        UserRegistration,
        AdminBroadcast,
        AdminDirectMessage {
  default boolean isUsingI18N() {
    return true;
  }

  String messageKey();

  @Builder
  record MeetupCreated(String title, UUID meetupId, URL meetupUrl) implements NotificationPayload {
    @Override
    public String messageKey() {
      return "notification.meetup.created";
    }
  }

  @Builder
  record JoinRequestCreated(String requesterName, UUID joinRequestId, String title, URL meetupUrl)
      implements NotificationPayload {
    @Override
    public String messageKey() {
      return "notification.join-request.new";
    }
  }

  @Builder
  record JoinRequestApproved(
      ZonedDateTime eventDate, String title, UUID joinRequestId, URL meetupUrl)
      implements NotificationPayload {
    @Override
    public String messageKey() {
      return "notification.join-request.approved";
    }
  }

  @Builder
  record MeetupRescheduled(String title, UUID meetupId, ZonedDateTime newEventDate, URL meetupUrl)
      implements NotificationPayload {
    @Override
    public String messageKey() {
      return "notification.meetup.rescheduled";
    }
  }

  @Builder
  record MeetupCanceled(String title, UUID meetupId, URL meetupUrl) implements NotificationPayload {
    @Override
    public String messageKey() {
      return "notification.meetup.canceled";
    }
  }

  @Builder
  record UserRegistration(String username) implements NotificationPayload {
    @Override
    public String messageKey() {
      return "notification.user.new";
    }
  }

  @Builder
  record UserMessengerLinked(String username) implements NotificationPayload {
    @Override
    public String messageKey() {
      return "notification.user.messenger.linked";
    }
  }

  @Builder
  record UserMessengerLinkingFailed(String username) implements NotificationPayload {
    @Override
    public String messageKey() {
      return "notification.user.messenger.link-failed";
    }
  }

  @Builder
  record UserApproved() implements NotificationPayload {
    @Override
    public String messageKey() {
      return "notification.user.approved";
    }
  }

  @Builder
  record AdminBroadcast(String adminName, UUID adminId, String message)
      implements NotificationPayload {
    @Override
    public String messageKey() {
      return message;
    }

    @Override
    public boolean isUsingI18N() {
      return false;
    }
  }

  @Builder
  record AdminDirectMessage(String adminName, UUID adminId, String message)
      implements NotificationPayload {
    @Override
    public String messageKey() {
      return message;
    }

    @Override
    public boolean isUsingI18N() {
      return false;
    }
  }
}

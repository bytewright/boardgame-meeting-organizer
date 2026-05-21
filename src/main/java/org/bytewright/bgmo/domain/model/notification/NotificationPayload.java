package org.bytewright.bgmo.domain.model.notification;

import static org.bytewright.bgmo.domain.model.notification.NotificationPayload.*;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Set;
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

  static Set<String> allMessageKeys() {
    return Set.of(
        MeetupCreated.MESSAGE_KEY,
        JoinRequestCreated.MESSAGE_KEY,
        JoinRequestApproved.MESSAGE_KEY,
        MeetupRescheduled.MESSAGE_KEY,
        MeetupCanceled.MESSAGE_KEY,
        UserRegistration.MESSAGE_KEY,
        UserMessengerLinked.MESSAGE_KEY,
        UserMessengerLinkingFailed.MESSAGE_KEY,
        UserApproved.MESSAGE_KEY);
  }

  @Builder
  record MeetupCreated(String title, UUID meetupId, URL meetupUrl) implements NotificationPayload {

    static final String MESSAGE_KEY = "notification.meetup.created";

    @Override
    public String messageKey() {
      return MESSAGE_KEY;
    }
  }

  @Builder
  record JoinRequestCreated(String requesterName, UUID joinRequestId, String title, URL meetupUrl)
      implements NotificationPayload {

    static final String MESSAGE_KEY = "notification.join-request.new";

    @Override
    public String messageKey() {
      return MESSAGE_KEY;
    }
  }

  @Builder
  record JoinRequestApproved(
      ZonedDateTime eventDate, String title, UUID joinRequestId, URL meetupUrl)
      implements NotificationPayload {

    static final String MESSAGE_KEY = "notification.join-request.approved";

    @Override
    public String messageKey() {
      return MESSAGE_KEY;
    }
  }

  @Builder
  record MeetupRescheduled(String title, UUID meetupId, ZonedDateTime newEventDate, URL meetupUrl)
      implements NotificationPayload {

    static final String MESSAGE_KEY = "notification.meetup.rescheduled";

    @Override
    public String messageKey() {
      return MESSAGE_KEY;
    }
  }

  @Builder
  record MeetupCanceled(String title, UUID meetupId, URL meetupUrl) implements NotificationPayload {

    static final String MESSAGE_KEY = "notification.meetup.canceled";

    @Override
    public String messageKey() {
      return MESSAGE_KEY;
    }
  }

  @Builder
  record UserRegistration(String username) implements NotificationPayload {

    static final String MESSAGE_KEY = "notification.user.new";

    @Override
    public String messageKey() {
      return MESSAGE_KEY;
    }
  }

  @Builder
  record UserMessengerLinked(String username) implements NotificationPayload {

    static final String MESSAGE_KEY = "notification.user.messenger.linked";

    @Override
    public String messageKey() {
      return MESSAGE_KEY;
    }
  }

  @Builder
  record UserMessengerLinkingFailed(String username) implements NotificationPayload {

    static final String MESSAGE_KEY = "notification.user.messenger.link-failed";

    @Override
    public String messageKey() {
      return MESSAGE_KEY;
    }
  }

  @Builder
  record UserApproved() implements NotificationPayload {

    static final String MESSAGE_KEY = "notification.user.approved";

    @Override
    public String messageKey() {
      return MESSAGE_KEY;
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

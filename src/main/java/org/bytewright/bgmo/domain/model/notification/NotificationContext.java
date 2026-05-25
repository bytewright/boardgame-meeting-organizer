package org.bytewright.bgmo.domain.model.notification;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.*;
import lombok.Builder;
import org.bytewright.bgmo.domain.model.JoinRequestPayload;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;

@Builder
public record NotificationContext(Content payload, Target target, Locale locale) {
  public NotificationContext(Content payload, Target target, Locale locale) {
    this.payload = Objects.requireNonNull(payload);
    this.target = Objects.requireNonNull(target);
    this.locale = Objects.requireNonNull(locale);
  }

  public Optional<NotificationChannel> extractChannel() {
    return switch (target) {
      case NotificationContext.Target.Anon anon -> Optional.of(anon.channel());
      case NotificationContext.Target.User user -> Optional.of(user.channel());
      case NotificationContext.Target.Group ignore -> Optional.empty();
    };
  }

  public sealed interface Target permits Target.Anon, Target.User, Target.Group {
    @Builder
    record Anon(String displayName, NotificationChannel channel) implements Target {}

    @Builder
    record User(UUID userId, String displayName, NotificationChannel channel) implements Target {}

    @Builder
    record Group() implements Target {}
  }

  public sealed interface Content
      permits Content.JoinRequestApproved,
          Content.JoinRequestCreated,
          Content.MeetupCanceled,
          Content.MeetupCreated,
          Content.MeetupRescheduled,
          Content.UserApproved,
          Content.UserMessengerLinked,
          Content.UserMessengerLinkingFailed,
          Content.UserRegistration,
          Content.AdminBroadcast,
          Content.AdminDirectMessage {

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
    record MeetupCreated(String title, UUID meetupId, URL meetupUrl) implements Content {

      static final String MESSAGE_KEY = "notification.meetup.created";

      @Override
      public String messageKey() {
        return MESSAGE_KEY;
      }
    }

    @Builder
    record JoinRequestCreated(String requesterName, UUID joinRequestId, String title, URL meetupUrl)
        implements Content {

      static final String MESSAGE_KEY = "notification.join-request.new";

      @Override
      public String messageKey() {
        return MESSAGE_KEY;
      }
    }

    @Builder
    record JoinRequestApproved(
        ZonedDateTime eventDate, String title, UUID joinRequestId, URL meetupUrl)
        implements Content {

      static final String MESSAGE_KEY = "notification.join-request.approved";

      @Override
      public String messageKey() {
        return MESSAGE_KEY;
      }
    }

    @Builder
    record MeetupRescheduled(String title, UUID meetupId, ZonedDateTime newEventDate, URL meetupUrl)
        implements Content {

      static final String MESSAGE_KEY = "notification.meetup.rescheduled";

      @Override
      public String messageKey() {
        return MESSAGE_KEY;
      }
    }

    @Builder
    record MeetupCanceled(String title, UUID meetupId, URL meetupUrl) implements Content {

      static final String MESSAGE_KEY = "notification.meetup.canceled";

      @Override
      public String messageKey() {
        return MESSAGE_KEY;
      }
    }

    @Builder
    record UserRegistration(String username) implements Content {

      static final String MESSAGE_KEY = "notification.user.new";

      @Override
      public String messageKey() {
        return MESSAGE_KEY;
      }
    }

    @Builder
    record UserMessengerLinked(String username) implements Content {

      static final String MESSAGE_KEY = "notification.user.messenger.linked";

      @Override
      public String messageKey() {
        return MESSAGE_KEY;
      }
    }

    @Builder
    record UserMessengerLinkingFailed(String username) implements Content {

      static final String MESSAGE_KEY = "notification.user.messenger.link-failed";

      @Override
      public String messageKey() {
        return MESSAGE_KEY;
      }
    }

    @Builder
    record UserApproved() implements Content {

      static final String MESSAGE_KEY = "notification.user.approved";

      @Override
      public String messageKey() {
        return MESSAGE_KEY;
      }
    }

    @Builder
    record AdminBroadcast(String adminName, UUID adminId, String message) implements Content {

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
    record AdminDirectMessage(String adminName, UUID adminId, String message) implements Content {

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

  public static class NotificationContextBuilder {
    public NotificationContextBuilder anonTarget(JoinRequestPayload.AnonEmail anonEmail) {
      return target(
          NotificationContext.Target.Anon.builder()
              .displayName(anonEmail.displayName())
              .channel(new NotificationChannel.Email(anonEmail.emailContact().email()))
              .build());
    }

    public NotificationContextBuilder userTarget(RegisteredUser user) {
      return target(
          NotificationContext.Target.User.builder()
              .userId(user.id())
              .displayName(user.getDisplayName())
              .channel(user.getNotificationChannel())
              .build());
    }
  }
}

package org.bytewright.bgmo.domain.model.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
  EVENT_NEW("notification.meetup.created"),
  EVENT_RESCHEDULED("notification.meetup.rescheduled"),
  EVENT_CANCELED("notification.meetup.canceled"),
  USER_APPROVED("notification.user.approved"),
  USER_REGISTRATION("notification.user.new"),
  JOIN_REQUEST_CREATED("notification.join-request.new"),
  JOIN_REQUEST_APPROVED("notification.join-request.approved");
  private final String messageKey;
}

package org.bytewright.bgmo.domain.model.notification;

import java.util.Locale;
import java.util.Map;
import lombok.Builder;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;

@Builder
public record NotificationContext(
    String messageKey, // e.g., "notification.meetup.created"
    Object[] messageArgs,
    String targetId, // Can be a User's ChatID or the Group ChatID
    ContactInfoType type,
    Locale locale,
    Map<String, String> metadata // For things like meetupId to build buttons
    ) {}

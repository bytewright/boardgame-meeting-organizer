package org.bytewright.bgmo.domain.model.notification;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.Builder;
import lombok.Singular;

@Builder
public record NotificationContext(
    NotificationType notificationType,
    String messageKey, // e.g., "notification.meetup.created"
    @Singular List<Object> messageArgs,
    NotificationTargetType notificationTargetType,
    Locale locale,
    @Nullable UUID meetupId,
    @Nullable UUID userId) {}

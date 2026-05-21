package org.bytewright.bgmo.domain.model.notification;

import jakarta.annotation.Nullable;
import java.util.Locale;
import java.util.UUID;
import lombok.Builder;

/** userId is only null for type==group */
@Builder
public record NotificationContext(
    NotificationPayload payload,
    NotificationTargetType notificationTargetType,
    Locale locale,
    @Nullable UUID userId) {}

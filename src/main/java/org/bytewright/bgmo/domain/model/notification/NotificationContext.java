package org.bytewright.bgmo.domain.model.notification;

import java.util.Locale;
import java.util.UUID;
import lombok.Builder;
import org.bytewright.bgmo.domain.model.user.ContactInfo;

/** userId is only null for type==group */
@Builder
public record NotificationContext(NotificationPayload payload, Target target, Locale locale) {

  public sealed interface Target permits Target.Anon, Target.User, Target.Group {
    @Builder
    record Anon(String displayName, ContactInfo contactInfo) implements Target {}

    @Builder
    record User(UUID userId, String displayName, ContactInfo primaryContactInfo)
        implements Target {}

    @Builder
    record Group() implements Target {}
  }
}

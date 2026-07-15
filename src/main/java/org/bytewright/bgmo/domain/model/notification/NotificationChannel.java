package org.bytewright.bgmo.domain.model.notification;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = NotificationChannel.None.class, name = "NONE"),
  @JsonSubTypes.Type(value = NotificationChannel.Email.class, name = "EMAIL"),
  @JsonSubTypes.Type(value = NotificationChannel.Discord.class, name = "DISCORD"),
  @JsonSubTypes.Type(value = NotificationChannel.Telegram.class, name = "MESSENGER_TELEGRAM")
})
public sealed interface NotificationChannel
    permits NotificationChannel.Discord,
        NotificationChannel.Email,
        NotificationChannel.None,
        NotificationChannel.Telegram {
  record None() implements NotificationChannel {}

  record Email(String email) implements NotificationChannel {}

  record Telegram(Long chatId, Long userId) implements NotificationChannel {}

  record Discord(long userId) implements NotificationChannel {}
}

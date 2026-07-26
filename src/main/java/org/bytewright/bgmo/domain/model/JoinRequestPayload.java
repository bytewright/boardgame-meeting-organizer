package org.bytewright.bgmo.domain.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.UUID;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = JoinRequestPayload.User.class, name = "USER"),
  @JsonSubTypes.Type(value = JoinRequestPayload.Anon.class, name = "ANON"),
  @JsonSubTypes.Type(value = JoinRequestPayload.AnonEmail.class, name = "ANON_EMAIL"),
  @JsonSubTypes.Type(
      value = JoinRequestPayload.NotificationChannelAnonUser.class,
      name = "CHANNEL_ANON_USER")
})
public sealed interface JoinRequestPayload
    permits JoinRequestPayload.Anon,
        JoinRequestPayload.AnonEmail,
        JoinRequestPayload.User,
        JoinRequestPayload.NotificationChannelAnonUser {

  static String displayName(RegisteredUserDao userDao, JoinRequestPayload payload) {
    return switch (payload) {
      case JoinRequestPayload.User u -> userDao.findOrThrow(u.userId()).getDisplayName();
      case JoinRequestPayload.Anon a -> a.displayName();
      case JoinRequestPayload.AnonEmail ae -> ae.displayName();
      case JoinRequestPayload.NotificationChannelAnonUser nc -> nc.displayName();
    };
  }

  static boolean isUser(JoinRequestPayload payload) {
    return payload instanceof User;
  }

  record User(UUID userId, ContactInfo contactInfo) implements JoinRequestPayload {}

  record Anon(String displayName, UUID anonToken, String contactInfo)
      implements JoinRequestPayload {}

  record AnonEmail(String displayName, UUID anonToken, ContactInfo.EmailContact emailContact)
      implements JoinRequestPayload {}

  // todo maybe refactor this to use ContactInfo interface
  record NotificationChannelAnonUser(
      ContactInfoType contactType, String displayName, String channelSpecificIdentifier)
      implements JoinRequestPayload {}
}

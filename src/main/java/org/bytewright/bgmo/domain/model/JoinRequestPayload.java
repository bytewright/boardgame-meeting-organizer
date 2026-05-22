package org.bytewright.bgmo.domain.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.UUID;
import org.bytewright.bgmo.domain.model.user.ContactInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = JoinRequestPayload.User.class, name = "USER"),
  @JsonSubTypes.Type(value = JoinRequestPayload.Anon.class, name = "ANON"),
  @JsonSubTypes.Type(value = JoinRequestPayload.AnonEmail.class, name = "ANON_EMAIL")
})
public sealed interface JoinRequestPayload
    permits JoinRequestPayload.Anon, JoinRequestPayload.AnonEmail, JoinRequestPayload.User {
  static boolean isUser(JoinRequestPayload payload) {
    return payload instanceof User;
  }

  record User(UUID userId, ContactInfo contactInfo) implements JoinRequestPayload {}

  record Anon(String displayName, UUID anonToken, String contactInfo)
      implements JoinRequestPayload {}

  record AnonEmail(String displayName, UUID anonToken, ContactInfo.EmailContact emailContact)
      implements JoinRequestPayload {}
}

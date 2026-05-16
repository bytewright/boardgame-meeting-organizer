package org.bytewright.bgmo.domain.model.user;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = ContactInfo.PhoneContact.class, name = "PHONE"),
  @JsonSubTypes.Type(value = ContactInfo.EmailContact.class, name = "EMAIL"),
  @JsonSubTypes.Type(value = ContactInfo.AddressContact.class, name = "ADR"),
  @JsonSubTypes.Type(value = ContactInfo.SignalContact.class, name = "MESSENGER_SIGNAL"),
  @JsonSubTypes.Type(value = ContactInfo.TelegramContact.class, name = "MESSENGER_TELEGRAM")
})
public sealed interface ContactInfo
    permits ContactInfo.PhoneContact,
        ContactInfo.EmailContact,
        ContactInfo.AddressContact,
        ContactInfo.SignalContact,
        ContactInfo.TelegramContact {

  ContactInfoType type();

  @Builder(toBuilder = true)
  record EmailContact(String email) implements ContactInfo {

    @Override
    public ContactInfoType type() {
      return ContactInfoType.EMAIL;
    }
  }

  /** deprecated, app doesn't need this and should be removed */
  @Builder(toBuilder = true)
  record AddressContact(
      String nameOnBell, String street, String zipCode, String city, String comment)
      implements ContactInfo {
    @Override
    public ContactInfoType type() {
      return ContactInfoType.ADDRESS;
    }
  }

  @Builder(toBuilder = true)
  record SignalContact(String signalHandle) implements ContactInfo {
    @Override
    public ContactInfoType type() {
      return ContactInfoType.SIGNAL;
    }
  }

  @Builder(toBuilder = true)
  record TelegramContact(String chatId, String telegramUsername) implements ContactInfo {
    @Override
    public ContactInfoType type() {
      return ContactInfoType.TELEGRAM;
    }
  }

  @Builder(toBuilder = true)
  record PhoneContact(String phoneNr) implements ContactInfo {
    @Override
    public ContactInfoType type() {
      return ContactInfoType.PHONE;
    }
  }
}

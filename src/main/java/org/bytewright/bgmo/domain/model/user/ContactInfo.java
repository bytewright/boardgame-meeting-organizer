package org.bytewright.bgmo.domain.model.user;

import java.util.UUID;
import lombok.Builder;
import org.bytewright.bgmo.domain.model.data.HasUUID;

public sealed interface ContactInfo extends HasUUID
    permits ContactInfo.PhoneContact,
        ContactInfo.EmailContact,
        ContactInfo.AddressContact,
        ContactInfo.SignalContact,
        ContactInfo.TelegramContact {
  UUID id();

  UUID userId();

  ContactInfoType type();

  ContactInfo withUserId(UUID userId);

  @Builder(toBuilder = true)
  record EmailContact(UUID id, UUID userId, String email) implements ContactInfo {
    @Override
    public ContactInfoType type() {
      return ContactInfoType.EMAIL;
    }

    @Override
    public ContactInfo withUserId(UUID userId) {
      return toBuilder().userId(userId).build();
    }
  }

  @Builder(toBuilder = true)
  record AddressContact(
      UUID id,
      UUID userId,
      String nameOnBell,
      String street,
      String zipCode,
      String city,
      String comment)
      implements ContactInfo {
    @Override
    public ContactInfoType type() {
      return ContactInfoType.ADDRESS;
    }

    @Override
    public ContactInfo withUserId(UUID userId) {
      return toBuilder().userId(userId).build();
    }
  }

  @Builder(toBuilder = true)
  record SignalContact(UUID id, UUID userId, String signalHandle) implements ContactInfo {
    @Override
    public ContactInfoType type() {
      return ContactInfoType.SIGNAL;
    }

    @Override
    public ContactInfo withUserId(UUID userId) {
      return toBuilder().userId(userId).build();
    }
  }

  @Builder(toBuilder = true)
  record TelegramContact(UUID id, UUID userId, String telegramHandle) implements ContactInfo {
    @Override
    public ContactInfoType type() {
      return ContactInfoType.TELEGRAM;
    }

    @Override
    public ContactInfo withUserId(UUID userId) {
      return toBuilder().userId(userId).build();
    }
  }

  @Builder(toBuilder = true)
  record PhoneContact(UUID id, UUID userId, String phoneNr) implements ContactInfo {
    @Override
    public ContactInfoType type() {
      return ContactInfoType.PHONE;
    }

    @Override
    public ContactInfo withUserId(UUID userId) {
      return toBuilder().userId(userId).build();
    }
  }
}

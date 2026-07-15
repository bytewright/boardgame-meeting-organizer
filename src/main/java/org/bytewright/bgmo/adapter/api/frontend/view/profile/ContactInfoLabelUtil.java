package org.bytewright.bgmo.adapter.api.frontend.view.profile;

import java.util.stream.Stream;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;

public class ContactInfoLabelUtil {
  static String messengerName(ContactInfoType type) {
    return switch (type) {
      case TELEGRAM -> "Telegram";
      case SIGNAL -> "Signal";
      case DISCORD -> "Discord";
      default -> type.name();
    };
  }

  static String labelFor(ContactInfoType type) {
    return switch (type) {
      case TELEGRAM -> "Telegram";
      case DISCORD -> "Discord";
      case SIGNAL -> "Signal";
      case EMAIL -> "Email";
      case PHONE -> "Phone";
      case ADDRESS -> "Address";
    };
  }

  static String translationKeyForAdd(ContactInfoType type) {
    return switch (type) {
      case EMAIL -> "profile.contacts.email.add";
      case TELEGRAM -> "profile.contacts.telegram.add";
      case DISCORD -> "profile.contacts.discord.add";
      case SIGNAL -> "profile.contacts.signal.add";
      case ADDRESS -> "profile.contacts.address.add";
      case PHONE -> "profile.contacts.phone.add";
    };
  }

  static String translationKeyForExplain(ContactInfoType type) {
    return switch (type) {
      case EMAIL -> "profile.contacts.email.helper";
      case TELEGRAM -> "profile.contacts.telegram.helper";
      case DISCORD -> "profile.contacts.discord.helper";
      case SIGNAL -> "profile.contacts.signal.helper";
      case ADDRESS -> "profile.contacts.address.helper";
      case PHONE -> "profile.contacts.phone.helper";
    };
  }

  static String displayValue(ContactInfo contact) {
    return switch (contact) {
      case ContactInfo.EmailContact e -> e.email();
      case ContactInfo.PhoneContact p -> p.phoneNr();
      case ContactInfo.TelegramContact t -> t.username();
      case ContactInfo.DiscordContact d -> d.username();
      case ContactInfo.SignalContact s -> s.signalHandle();
      case ContactInfo.AddressContact a ->
          Stream.of(a.nameOnBell(), a.street(), a.zipCode() + " " + a.city())
              .filter(v -> v != null && !v.isBlank())
              .reduce((x, y) -> x + ", " + y)
              .orElse("—");
    };
  }
}

package org.bytewright.bgmo.adapter.api.frontend.view.profile;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.util.stream.Stream;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;

public class ContactInfoLabelUtil {
  static String messengerName(ContactInfoType type) {
    return switch (type) {
      case TELEGRAM -> "Telegram";
      case SIGNAL -> "Signal";
      default -> type.name();
    };
  }

  static String labelFor(ContactInfoType type) {
    return switch (type) {
      case TELEGRAM -> "Telegram";
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
      case SIGNAL -> "profile.contacts.signal.add";
      case ADDRESS -> "profile.contacts.address.add";
      case PHONE -> "profile.contacts.phone.add";
    };
  }

  static String translationKeyForExplain(ContactInfoType type) {
    return switch (type) {
      case EMAIL -> "profile.contacts.email.helper";
      case TELEGRAM -> "profile.contacts.telegram.helper";
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
      case ContactInfo.SignalContact s -> s.signalHandle();
      case ContactInfo.AddressContact a ->
          Stream.of(a.nameOnBell(), a.street(), a.zipCode() + " " + a.city())
              .filter(v -> v != null && !v.isBlank())
              .reduce((x, y) -> x + ", " + y)
              .orElse("—");
    };
  }

  public static Component typeIcon(ContactInfoType type) {
    return switch (type) {
      case EMAIL -> VaadinIcon.MAILBOX.create();
      case TELEGRAM -> VaadinIcon.TEXT_INPUT.create();
      case SIGNAL -> VaadinIcon.TEXT_INPUT.create();
      case ADDRESS -> VaadinIcon.HOME.create();
      case PHONE -> VaadinIcon.PHONE.create();
    };
  }
}

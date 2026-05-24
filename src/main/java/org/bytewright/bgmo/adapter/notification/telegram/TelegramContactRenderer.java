package org.bytewright.bgmo.adapter.notification.telegram;

import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.springframework.stereotype.Service;

@Service
public class TelegramContactRenderer {

  public String render(String displayName, ContactInfo contactInfo) {
    return switch (contactInfo) {
      case ContactInfo.TelegramContact tc ->
          "[%s](https://t.me/%s)".formatted(displayName, tc.telegramUsername());
      case ContactInfo.EmailContact ec -> "[%s](mailto:%s)".formatted(displayName, ec.email());
      case ContactInfo.PhoneContact pc -> "[%s](tel:%s)".formatted(displayName, pc.phoneNr());
      case ContactInfo.SignalContact ignored -> displayName; // no universal deeplink scheme
      case ContactInfo.AddressContact ignored -> displayName;
    };
  }
}

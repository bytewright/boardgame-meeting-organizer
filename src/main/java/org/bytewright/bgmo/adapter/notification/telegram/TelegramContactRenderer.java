package org.bytewright.bgmo.adapter.notification.telegram;

import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactOption;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.springframework.stereotype.Service;

@Service
public class TelegramContactRenderer {

  public String render(RegisteredUser creator) {
    return creator
        .resolvePrimaryContact()
        .map(ContactOption::getContactInfo)
        .map(contactInfo -> render(creator.getDisplayName(), contactInfo))
        .orElse(creator.getDisplayName());
  }

  public String render(String displayName, ContactInfo contactInfo) {
    return switch (contactInfo) {
      case ContactInfo.TelegramContact tc ->
          "[%s](tg://user/%s)".formatted(displayName, tc.username());
      case ContactInfo.EmailContact ec -> "[%s](mailto:%s)".formatted(displayName, ec.email());
      case ContactInfo.PhoneContact pc -> "[%s](tel:%s)".formatted(displayName, pc.phoneNr());
      case ContactInfo.SignalContact ignored -> displayName; // no universal deeplink scheme
      case ContactInfo.DiscordContact dc -> dc.username();
      case ContactInfo.AddressContact ignored -> displayName;
    };
  }
}

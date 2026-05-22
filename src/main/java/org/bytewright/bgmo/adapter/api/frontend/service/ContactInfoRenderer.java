package org.bytewright.bgmo.adapter.api.frontend.service;

import com.vaadin.flow.component.html.Span;
import lombok.RequiredArgsConstructor;
import org.bytewright.bgmo.domain.model.JoinRequestPayload;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContactInfoRenderer {

  public Span render(MeetupJoinRequest req) {
    switch (req.getPayload()) {
      case JoinRequestPayload.Anon anon -> {
        return renderAnon(anon);
      }
      case JoinRequestPayload.User user -> {
        return renderUser(user);
      }
      case JoinRequestPayload.AnonEmail anonEmail -> {
        return renderAnonEmail(anonEmail);
      }
    }
  }

  private Span renderAnonEmail(JoinRequestPayload.AnonEmail anonEmail) {
    Span contactValue = new Span("✉ " + anonEmail.emailContact().email());
    contactValue.getStyle().set("word-break", "break-word");
    return contactValue;
  }

  private Span renderUser(JoinRequestPayload.User user) {
    String contactString =
        switch (user.contactInfo()) {
          case ContactInfo.AddressContact ignore -> throw new IllegalArgumentException("");
          case ContactInfo.EmailContact emailContact -> "✉ " + emailContact.email();
          case ContactInfo.PhoneContact phoneContact -> "☎ " + phoneContact.phoneNr();
          case ContactInfo.SignalContact signalContact ->
              "Signaluser: " + signalContact.signalHandle();
          case ContactInfo.TelegramContact telegramContact ->
              "Telegramuser: " + telegramContact.telegramUsername();
        };
    Span contactValue = new Span(contactString);
    contactValue.getStyle().set("word-break", "break-word");
    return contactValue;
  }

  private Span renderAnon(JoinRequestPayload.Anon anon) {
    Span contactValue = new Span(anon.contactInfo());
    contactValue.getStyle().set("word-break", "break-word");
    return contactValue;
  }
}

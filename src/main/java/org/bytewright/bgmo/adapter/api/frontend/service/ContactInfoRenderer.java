package org.bytewright.bgmo.adapter.api.frontend.service;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Span;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.bytewright.bgmo.domain.model.JoinRequestPayload;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactOption;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.user.ContactInfoService;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

@Service
@RequiredArgsConstructor
public class ContactInfoRenderer {
  private final ContactInfoService contactInfoService;

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

  public Component renderAsDeeplink(MeetupEvent meetup, RegisteredUser creator) {
    ContactOption contactOption = contactInfoService.getPrimaryContact(creator).orElseThrow();
    return switch (contactOption.getContactInfo()) {
      case ContactInfo.EmailContact emailContact -> {
        Anchor anchor = new Anchor();
        String subject = meetup.getTitle();
        String mailBody =
            "Hi %s,\nfolgende Frage zu dem von dir erstellten Meetup '%s':\n"
                .formatted(creator.getDisplayName(), meetup.getTitle());
        anchor.setHref(
            "mailto:%s?subject=%s&body=%s"
                .formatted(
                    emailContact.email(),
                    UriUtils.encodeQueryParam(subject, StandardCharsets.UTF_8),
                    UriUtils.encodeQueryParam(mailBody, StandardCharsets.UTF_8)));
        anchor.setTarget(AnchorTarget.BLANK);
        anchor.setText("✉ %s".formatted(creator.getDisplayName()));
        yield anchor;
      }
      case ContactInfo.PhoneContact phoneContact -> {
        Anchor anchor = new Anchor();
        anchor.setHref("tel:%s".formatted(phoneContact.phoneNr()));
        anchor.setTarget(AnchorTarget.BLANK);
        anchor.setText("☎ %s".formatted(creator.getDisplayName()));
        yield anchor;
      }
      case ContactInfo.TelegramContact telegramContact -> {
        String draftText =
            "Hi %s,\nfolgende Frage zu dem von dir erstellten Meetup '%s':\n"
                .formatted(creator.getDisplayName(), meetup.getTitle());
        String href =
            "https://t.me/%s?text=%s"
                .formatted(
                    telegramContact.telegramUsername(),
                    URLEncoder.encode(draftText, StandardCharsets.UTF_8));
        Anchor anchor = new Anchor();
        anchor.setHref(href);
        anchor.setTarget(AnchorTarget.BLANK);
        anchor.setText("Telegram %s".formatted(creator.getDisplayName()));
        // todo hmm maybe this should use the telegram icon?
        //  meaning this would need to be a horizontalLayout?
        yield anchor;
      }
      case ContactInfo.SignalContact signalContact -> {
        // for later, not relevant
        throw new NotImplementedException();
      }
      case ContactInfo.AddressContact addressContact -> {
        // deprecated, type to be removed
        throw new IllegalArgumentException();
      }
    };
  }
}

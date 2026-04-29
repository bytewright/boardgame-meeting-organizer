package org.bytewright.bgmo.adapter.api.frontend.view.meetup.component;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.time.ZonedDateTime;
import org.bytewright.bgmo.adapter.api.frontend.service.MeetupDetailContext;
import org.bytewright.bgmo.adapter.api.frontend.service.i18n.LocaleService;
import org.bytewright.bgmo.adapter.api.frontend.view.component.GameTimeAndDuration;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.user.ContactInfo;

/**
 * Renders the public event header: title, date, time, creator, slot count, and the organiser's
 * address (zip or full, depending on the viewer's role).
 *
 * <p>Always visible to every visitor; never shows contact info beyond what the viewer's role
 * permits — address display is fully delegated to {@link MeetupDetailContext#showZipCode()} and
 * {@link MeetupDetailContext#showFullAddress()}.
 */
public class MeetupInfoHeader extends VerticalLayout {

  public MeetupInfoHeader(MeetupDetailContext ctx, LocaleService localeService) {
    setPadding(false);
    setSpacing(true);

    MeetupEvent meetup = ctx.meetup();

    // ── Canceled banner ──────────────────────────────────────────────────────
    if (meetup.isCanceled()) {
      Span canceledBadge = new Span(getTranslation("meetup.canceled"));
      canceledBadge
          .getStyle()
          .set("color", "var(--lumo-error-color)")
          .set("font-weight", "bold")
          .set("font-size", "1.1em");
      add(canceledBadge);
    }

    // ── Title ────────────────────────────────────────────────────────────────
    add(new H2(meetup.getTitle()));

    // ── Date row ─────────────────────────────────────────────────────────────
    ZonedDateTime eventDate = meetup.getEventDate();
    Icon calendarIcon = VaadinIcon.CALENDAR.create();
    calendarIcon.setSize("var(--lumo-icon-size-s)");
    calendarIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
    Span dateSpan = new Span(eventDate.format(localeService.getDateFormatter()));
    HorizontalLayout dateRow = new HorizontalLayout(calendarIcon, dateSpan);
    dateRow.setAlignItems(Alignment.CENTER);
    dateRow.setSpacing(false);
    dateRow.getStyle().set("gap", "var(--lumo-space-s)");
    add(dateRow);

    // ── Time + duration row ───────────────────────────────────────────────────
    String timeStr = eventDate.format(localeService.getTimeFormatter());
    add(new GameTimeAndDuration(timeStr, meetup.getDurationHours()));

    // ── Creator + slots row ───────────────────────────────────────────────────
    Icon personIcon = VaadinIcon.USER.create();
    personIcon.setSize("var(--lumo-icon-size-s)");
    personIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
    Span creatorSpan = new Span(ctx.creatorDisplayName());
    creatorSpan.setMinWidth(200, Unit.PIXELS);

    Icon slotsIcon = VaadinIcon.USERS.create();
    slotsIcon.setSize("var(--lumo-icon-size-s)");
    slotsIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
    Span slotsSpan = new Span(buildSlotsText(ctx));

    HorizontalLayout creatorAndSlots =
        new HorizontalLayout(personIcon, creatorSpan, slotsIcon, slotsSpan);
    creatorAndSlots.setAlignItems(Alignment.CENTER);
    add(creatorAndSlots);

    // ── Address block ─────────────────────────────────────────────────────────
    if (ctx.showFullAddress()) {
      ctx.creatorAddress().ifPresent(addr -> add(buildFullAddressBlock(addr)));
    } else if (ctx.showZipCode()) {
      ctx.zipCode()
          .ifPresent(
              zip -> {
                Icon locationIcon = VaadinIcon.MAP_MARKER.create();
                locationIcon.setSize("var(--lumo-icon-size-s)");
                locationIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
                Span zipSpan = new Span(getTranslation("meetup.address.area", zip));
                HorizontalLayout zipRow = new HorizontalLayout(locationIcon, zipSpan);
                zipRow.setAlignItems(Alignment.CENTER);
                zipRow.setSpacing(false);
                zipRow.getStyle().set("gap", "var(--lumo-space-s)");
                add(zipRow);
              });
    }

    // ── Description ──────────────────────────────────────────────────────────
    String desc =
        meetup.getDescription() != null && !meetup.getDescription().isBlank()
            ? meetup.getDescription()
            : getTranslation("meetup.no-desc");
    add(new Paragraph(desc));
  }

  private String buildSlotsText(MeetupDetailContext ctx) {
    MeetupEvent meetup = ctx.meetup();
    if (meetup.isUnlimitedSlots()) {
      return getTranslation("meetup.unlimitedSlots");
    }
    long accepted =
        meetup.getJoinRequests().stream()
            .filter(
                r -> r.getRequestState() == org.bytewright.bgmo.domain.model.RequestState.ACCEPTED)
            .count();
    return getTranslation("meetup.slotsFilled", accepted, meetup.getJoinSlots());
  }

  private VerticalLayout buildFullAddressBlock(ContactInfo.AddressContact addr) {
    VerticalLayout block = new VerticalLayout();
    block.setPadding(true);
    block.setSpacing(false);
    block
        .getStyle()
        .set("background", "var(--lumo-contrast-5pct)")
        .set("border-radius", "var(--lumo-border-radius-m)")
        .set("border-left", "3px solid var(--lumo-success-color)");

    Span label = new Span(getTranslation("meetup.address.full.label"));
    label
        .getStyle()
        .set("font-size", "var(--lumo-font-size-s)")
        .set("color", "var(--lumo-secondary-text-color)");

    Span name = new Span(addr.nameOnBell());
    name.getStyle().set("font-weight", "bold");

    Span street = new Span(addr.street());
    Span cityLine = new Span(addr.zipCode() + " " + addr.city());

    block.add(label, name, street, cityLine);

    if (addr.comment() != null && !addr.comment().isBlank()) {
      Span comment = new Span(addr.comment());
      comment
          .getStyle()
          .set("font-size", "var(--lumo-font-size-s)")
          .set("color", "var(--lumo-secondary-text-color)");
      block.add(comment);
    }

    return block;
  }
}

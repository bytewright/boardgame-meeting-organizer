package org.bytewright.bgmo.adapter.api.frontend.view.meetup.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.bytewright.bgmo.adapter.api.frontend.service.MeetupDetailContext;
import org.bytewright.bgmo.adapter.api.frontend.service.i18n.LocaleService;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MeetupBasicsInfo;
import org.bytewright.bgmo.domain.model.MeetupEvent;

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
    Button shareBtn = new Button(getTranslation("meetup.share"), VaadinIcon.SHARE.create());
    shareBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
    shareBtn.setAriaLabel(getTranslation("meetup.share"));
    shareBtn.addClickListener(
        e ->
            UI.getCurrent()
                .getPage()
                .executeJs(
                    """
        if (navigator.share) {
            navigator.share({ title: $0, url: window.location.href });
        } else {
            navigator.clipboard.writeText(window.location.href);
        }
    """,
                    meetup.getTitle()));
    H2 title = new H2(getTranslation("meetup.details.title"));
    HorizontalLayout titleRow = new HorizontalLayout(title, shareBtn);
    titleRow.setWidthFull();
    titleRow.setAlignItems(Alignment.CENTER);
    titleRow.expand(title);
    add(titleRow);
    MeetupBasicsInfo meetupBasicsInfo = new MeetupBasicsInfo(localeService);
    MeetupBasicsInfo.Context context =
        MeetupBasicsInfo.Context.builder()
            .meetup(meetup)
            .creator(ctx.creator())
            .changeCursor(false)
            .build();
    meetupBasicsInfo.buildComponent(context);
    add(meetupBasicsInfo);

    // ── Address block ─────────────────────────────────────────────────────────
    if (ctx.showFullAddress()) {
      add(buildFullAddressBlock(ctx));
    } else if (ctx.showZipCode()) {
      Icon locationIcon = VaadinIcon.MAP_MARKER.create();
      locationIcon.setSize("var(--lumo-icon-size-s)");
      locationIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
      Span zipSpan = new Span(ctx.meetup().getAreaHint());
      HorizontalLayout zipRow = new HorizontalLayout(locationIcon, zipSpan);
      zipRow.setAlignItems(Alignment.CENTER);
      zipRow.setSpacing(false);
      zipRow.getStyle().set("gap", "var(--lumo-space-s)");
      add(zipRow);
    }

    // ── Description ──────────────────────────────────────────────────────────
    String desc =
        meetup.getDescription() != null && !meetup.getDescription().isBlank()
            ? meetup.getDescription()
            : getTranslation("meetup.no-desc");
    Div descDiv = new Div();
    descDiv.setText(desc);
    descDiv
        .getStyle()
        .set("white-space", "pre-wrap") // honours \n, wraps long lines
        .set("word-break", "break-word") // no overflow on long unbroken strings
        .set("font-family", "inherit");
    add(descDiv);
  }

  private VerticalLayout buildFullAddressBlock(MeetupDetailContext ctx) {
    VerticalLayout block = new VerticalLayout();
    block.setPadding(true);
    block.setSpacing(false);
    block
        .getStyle()
        .set("background", "var(--lumo-contrast-5pct)")
        .set("border-radius", "var(--lumo-border-radius-m)")
        .set("border-left", "3px solid var(--lumo-success-color)");
    Icon locationIcon = VaadinIcon.MAP_MARKER.create();
    locationIcon.setSize("var(--lumo-icon-size-s)");
    locationIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
    Span zipSpan = new Span(ctx.meetup().getAreaHint());
    HorizontalLayout zipRow = new HorizontalLayout(locationIcon, zipSpan);
    zipRow.setAlignItems(Alignment.CENTER);
    zipRow.setSpacing(false);
    zipRow.getStyle().set("gap", "var(--lumo-space-s)");
    block.add(zipRow);
    Span label = new Span(getTranslation("meetup.address.full.label"));
    label
        .getStyle()
        .set("font-size", "var(--lumo-font-size-s)")
        .set("color", "var(--lumo-secondary-text-color)");

    Span name = new Span(ctx.meetup().getFullLocation());
    name.getStyle().set("font-weight", "bold");
    block.add(label, name);
    return block;
  }
}

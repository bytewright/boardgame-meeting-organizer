package org.bytewright.bgmo.adapter.api.frontend.view.component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.dom.Style;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.bytewright.bgmo.adapter.api.frontend.service.i18n.LocaleService;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.RequestState;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;

public class MeetupBasicsInfo extends Div {
  private final LocaleService localeService;
  private final MeetupEvent meetup;
  private final RegisteredUser creator;

  public MeetupBasicsInfo(LocaleService localeService, MeetupEvent meetup, RegisteredUser creator) {
    this.localeService = localeService;
    this.meetup = meetup;
    this.creator = creator;
    buildComponent();
  }

  private void buildComponent() {
    setWidthFull();
    getStyle()
        .setBorder("2px solid var(--lumo-contrast-20pct)")
        .setBorderRadius("var(--lumo-border-radius-l)")
        .setPadding("var(--lumo-space-m)")
        .setBoxSizing(Style.BoxSizing.BORDER_BOX)
        .setTransition("background-color 0.15s ease")
        .setCursor("pointer")
        .setPadding("var(--lumo-space-m)");

    Span title = new Span(meetup.getTitle());
    title
        .getStyle()
        .setDisplay(Style.Display.BLOCK)
        .setFontWeight(Style.FontWeight.BOLD)
        .setFontSize("var(--lumo-font-size-l)")
        .setMarginBottom("var(--lumo-space-xs)");

    // ── Row 2: Date ───────────────────────────────────────────────────────────
    ZonedDateTime eventDate = meetup.getEventDate();
    String dateStr = eventDate.format(localeService.getDateFormatter());
    HorizontalLayout dateRow = buildIconRow(VaadinIcon.CALENDAR, dateStr);

    // ── Row 3: Time + Duration ────────────────────────────────────────────────
    String timeStr = eventDate.format(localeService.getTimeFormatter());
    HorizontalLayout timeRow = new GameTimeAndDuration(timeStr, meetup.getDurationHours());

    // ── Row 4: Creator + Slots ────────────────────────────────────────────────
    String creatorName =
        Optional.of(creator)
            .map(RegisteredUser::getDisplayName)
            .orElseGet(() -> meetup.getCreatorId().toString());

    long usedSlots =
        meetup.getJoinRequests().stream()
            .filter(r -> RequestState.ACCEPTED == r.getRequestState())
            .count();
    String slotsText =
        meetup.isUnlimitedSlots()
            ? getTranslation("meetup.unlimitedSlots")
            : getTranslation("meetup.slotsFilled", usedSlots, meetup.getJoinSlots());

    Icon personIcon = VaadinIcon.USER.create();
    personIcon.setSize("var(--lumo-icon-size-s)");
    personIcon.getStyle().setColor("var(--lumo-secondary-text-color)");
    Span creatorSpan = new Span(creatorName);

    Icon slotsIcon = VaadinIcon.USERS.create();
    slotsIcon.setSize("var(--lumo-icon-size-s)");
    slotsIcon.getStyle().setColor("var(--lumo-secondary-text-color)");
    Span slotsSpan = new Span(slotsText);

    // Each icon+label pair is its own flex item, so they wrap together as a unit
    HorizontalLayout creatorGroup = new HorizontalLayout(personIcon, creatorSpan);
    creatorGroup.setSpacing(true);
    creatorGroup.setAlignItems(FlexComponent.Alignment.CENTER);
    creatorGroup.getStyle().set("flex", "1 1 0").setMinWidth("140px");

    HorizontalLayout slotsGroup = new HorizontalLayout(slotsIcon, slotsSpan);
    slotsGroup.setSpacing(true);
    slotsGroup.setAlignItems(FlexComponent.Alignment.CENTER);
    slotsGroup.getStyle().set("flex", "1 1 0").setMinWidth("140px");

    HorizontalLayout bottomRow = new HorizontalLayout(creatorGroup, slotsGroup);
    bottomRow.setSpacing(false);
    bottomRow.setAlignItems(FlexComponent.Alignment.CENTER);
    bottomRow.getStyle().setFlexWrap(Style.FlexWrap.WRAP);

    add(title, dateRow, timeRow, bottomRow);
  }

  /** Helper: single icon + text in a horizontal row. */
  private HorizontalLayout buildIconRow(VaadinIcon vaadinIcon, String text) {
    Icon icon = vaadinIcon.create();
    icon.setSize("var(--lumo-icon-size-s)");
    icon.getStyle().set("color", "var(--lumo-secondary-text-color)");

    HorizontalLayout row = new HorizontalLayout(icon, new Span(text));
    row.setSpacing(true);
    row.setAlignItems(FlexComponent.Alignment.CENTER);
    return row;
  }

  static class GameTimeAndDuration extends HorizontalLayout {

    public GameTimeAndDuration(String timeStr, int durationHours) {
      Icon clockIcon = VaadinIcon.CLOCK.create();
      clockIcon.setSize("var(--lumo-icon-size-s)");
      clockIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
      Span timeSpan = new Span(timeStr + " " + getTranslation("meetup.time.suffix"));

      Icon timerIcon = VaadinIcon.TIMER.create();
      timerIcon.setSize("var(--lumo-icon-size-s)");
      timerIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
      Span durationSpan = new Span(getTranslation("meetup.duration", durationHours));

      // Each icon+label pair is its own flex item, so they wrap together as a unit
      HorizontalLayout timeGroup = new HorizontalLayout(clockIcon, timeSpan);
      timeGroup.setSpacing(true);
      timeGroup.setAlignItems(Alignment.CENTER);
      timeGroup.getStyle().set("flex", "1 1 0").setMinWidth("140px");

      HorizontalLayout durationGroup = new HorizontalLayout(timerIcon, durationSpan);
      durationGroup.setSpacing(true);
      durationGroup.setAlignItems(Alignment.CENTER);
      durationGroup.getStyle().set("flex", "1 1 0").setMinWidth("140px");

      add(timeGroup, durationGroup);
      setSpacing(false);
      setAlignItems(Alignment.CENTER);
      // Pairs wrap to a new line on narrow screens
      getStyle().setFlexWrap(Style.FlexWrap.WRAP);
    }
  }
}

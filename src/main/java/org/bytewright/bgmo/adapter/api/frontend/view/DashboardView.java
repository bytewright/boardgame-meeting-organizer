package org.bytewright.bgmo.adapter.api.frontend.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.PermitAll;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.api.frontend.SessionAuthenticationService;
import org.bytewright.bgmo.adapter.api.frontend.service.i18n.LocaleService;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.RequestState;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.usecases.MeetupWorkflows;

@Slf4j
@Route(value = "dashboard", layout = MainLayout.class)
@RouteAlias("")
@RouteAlias("home")
@PageTitle("Dashboard | Boardgame Meeting Organizer")
@PermitAll
public class DashboardView extends VerticalLayout implements BeforeEnterObserver {

  private final LocaleService localeService;
  private final SessionAuthenticationService authService;
  private final MeetupWorkflows meetupWorkflows;
  private final MeetupDao meetupDao;
  private final RegisteredUserDao registeredUserDao;
  private RegisteredUser currentUser;

  public DashboardView(
      LocaleService localeService,
      SessionAuthenticationService authService,
      MeetupWorkflows meetupWorkflows,
      MeetupDao meetupDao,
      RegisteredUserDao registeredUserDao) {
    this.localeService = localeService;
    this.authService = authService;
    this.meetupWorkflows = meetupWorkflows;
    this.meetupDao = meetupDao;
    this.registeredUserDao = registeredUserDao;

    setSizeFull();
    setPadding(true);
    setSpacing(true);
    getStyle().set("max-width", MainLayout.MAX_DISPLAYPORT_WIDTH).set("margin", "0 auto");
  }

  private void buildUI() {
    removeAll();

    List<MeetupEvent> upcomingMeetups =
        meetupDao.findAll().stream()
            .filter(m -> !m.isCanceled())
            .filter(m -> m.getEventDate().isAfter(ZonedDateTime.now().minusDays(1)))
            .sorted(Comparator.comparing(MeetupEvent::getEventDate))
            .toList();

    if (upcomingMeetups.isEmpty()) {
      add(new Span(getTranslation("dashboard.no-meetups")));
      return;
    }

    for (MeetupEvent meetup : upcomingMeetups) {
      add(buildMeetupCard(meetup));
    }
  }

  private Div buildMeetupCard(MeetupEvent meetup) {
    Div card = new Div();
    card.getStyle()
        .set("border", "2px solid var(--lumo-contrast-20pct)")
        .set("border-radius", "var(--lumo-border-radius-l)")
        .set("padding", "var(--lumo-space-m)")
        .set("cursor", "pointer")
        .set("width", "100%")
        .set("box-sizing", "border-box")
        .set("transition", "background-color 0.15s ease");

    // Hover highlight using JS — lightweight approach with style toggle
    card.getElement()
        .addEventListener(
            "mouseover", e -> card.getStyle().set("background-color", "var(--lumo-contrast-5pct)"));
    card.getElement().addEventListener("mouseout", e -> card.getStyle().remove("background-color"));

    // ── Row 1: Title ─────────────────────────────────────────────────────────
    Span title = new Span(meetup.getTitle());
    title
        .getStyle()
        .set("font-size", "var(--lumo-font-size-l)")
        .set("font-weight", "bold")
        .set("display", "block")
        .set("margin-bottom", "var(--lumo-space-xs)");

    // ── Row 2: Date ───────────────────────────────────────────────────────────
    // Derive locale from the existing combined formatter so no new LocaleService method is needed.
    java.util.Locale locale = localeService.getFormatter().getLocale();
    String dateStr =
        meetup.getEventDate().format(DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy", locale));
    HorizontalLayout dateRow = buildIconRow(VaadinIcon.CALENDAR, dateStr);

    // ── Row 3: Time + Duration ────────────────────────────────────────────────
    String timeStr = meetup.getEventDate().format(DateTimeFormatter.ofPattern("HH:mm", locale));
    Span timeSpan = new Span(timeStr + " " + getTranslation("meetup.time.suffix"));
    Span durationSpan = new Span(getTranslation("meetup.duration", meetup.getDurationHours()));

    Icon clockIcon = VaadinIcon.CLOCK.create();
    clockIcon.setSize("var(--lumo-icon-size-s)");
    clockIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");

    Icon timerIcon = VaadinIcon.TIMER.create();
    timerIcon.setSize("var(--lumo-icon-size-s)");
    timerIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");

    HorizontalLayout timeRow = new HorizontalLayout(clockIcon, timeSpan, timerIcon, durationSpan);
    timeRow.setSpacing(true);
    timeRow.setAlignItems(Alignment.CENTER);

    // ── Row 4: Creator + Slots ────────────────────────────────────────────────
    String creatorName =
        registeredUserDao
            .findById(meetup.getCreatorId())
            .map(RegisteredUser::getDisplayName)
            .orElseGet(() -> meetup.getCreatorId().toString());

    String slotsText =
        meetup.isUnlimitedSlots()
            ? getTranslation("meetup.unlimitedSlots")
            : getTranslation(
                "meetup.slotsFilled",
                meetup.getJoinRequests().stream()
                    .filter(r -> RequestState.ACCEPTED == r.getRequestState())
                    .count(),
                meetup.getJoinSlots());

    Icon personIcon = VaadinIcon.USER.create();
    personIcon.setSize("var(--lumo-icon-size-s)");
    personIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");

    Span creatorSpan = new Span(creatorName);

    Icon slotsIcon = VaadinIcon.TICKET.create();
    slotsIcon.setSize("var(--lumo-icon-size-s)");
    slotsIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");

    Span slotsSpan = new Span(slotsText);

    HorizontalLayout bottomRow =
        new HorizontalLayout(personIcon, creatorSpan, slotsIcon, slotsSpan);
    bottomRow.setSpacing(true);
    bottomRow.setAlignItems(Alignment.CENTER);

    // ── Divider between rows ──────────────────────────────────────────────────
    Div divider = new Div();
    divider
        .getStyle()
        .set("border-top", "1px solid var(--lumo-contrast-10pct)")
        .set("margin", "var(--lumo-space-xs) 0");

    card.add(title, dateRow, divider, timeRow, bottomRow);

    // ── Navigate to detail on click ───────────────────────────────────────────
    card.addClickListener(
        e ->
            UI.getCurrent()
                .navigate(
                    MeetupDetailView.class, new RouteParam("meetupId", meetup.getId().toString())));

    return card;
  }

  /** Helper: single icon + text in a horizontal row. */
  private HorizontalLayout buildIconRow(VaadinIcon vaadinIcon, String text) {
    Icon icon = vaadinIcon.create();
    icon.setSize("var(--lumo-icon-size-s)");
    icon.getStyle().set("color", "var(--lumo-secondary-text-color)");

    HorizontalLayout row = new HorizontalLayout(icon, new Span(text));
    row.setSpacing(true);
    row.setAlignItems(Alignment.CENTER);
    return row;
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Optional<RegisteredUser> userOpt = authService.getCurrentUser();
    if (userOpt.isEmpty()) {
      log.debug("User is not logged in, redirecting to log-in view");
      event.forwardTo(LoginView.class);
      return;
    }
    this.currentUser = userOpt.get();
    buildUI();
  }
}

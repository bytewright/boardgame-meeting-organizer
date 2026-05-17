package org.bytewright.bgmo.adapter.api.frontend.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.PermitAll;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.api.frontend.service.SessionInfoService;
import org.bytewright.bgmo.adapter.api.frontend.service.i18n.LocaleService;
import org.bytewright.bgmo.adapter.api.frontend.view.component.GameTimeAndDuration;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.adapter.api.frontend.view.meetup.MeetupDetailView;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.RequestState;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.usecases.MeetupWorkflows;

@Slf4j
@Route(value = "dashboard", layout = MainLayout.class)
@RouteAlias(value = "/", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@RouteAlias(value = "home", layout = MainLayout.class)
@PageTitle("Dashboard | Boardgame Meeting Organizer")
@PermitAll
public class DashboardView extends VerticalLayout implements BeforeEnterObserver {

  private final LocaleService localeService;
  private final SessionInfoService authService;
  private final MeetupWorkflows meetupWorkflows;
  private final RegisteredUserDao registeredUserDao;

  public DashboardView(
      LocaleService localeService,
      SessionInfoService authService,
      MeetupWorkflows meetupWorkflows,
      RegisteredUserDao registeredUserDao) {
    this.localeService = localeService;
    this.authService = authService;
    this.meetupWorkflows = meetupWorkflows;
    this.registeredUserDao = registeredUserDao;

    setWidthFull();
    setPadding(true);
    setSpacing(true);
    setMaxWidth(MainLayout.MAX_DISPLAYPORT_WIDTH);
    getStyle().set("margin", "0 auto");
  }

  private void buildUI(UUID currentUserId) {
    removeAll();
    List<MeetupEvent> meetupsFromCurrentUser =
        meetupWorkflows.findMeetupsByOrganizer(currentUserId);
    List<MeetupEvent> upcomingMeetups =
        meetupWorkflows.findPubliclyListed().stream()
            .filter(meetupEvent -> !meetupsFromCurrentUser.contains(meetupEvent))
            .toList();

    if (meetupsFromCurrentUser.isEmpty() && upcomingMeetups.isEmpty()) {
      add(new Span(getTranslation("dashboard.no-meetups")));
    } else {
      if (!meetupsFromCurrentUser.isEmpty()) {
        add(new H3(getTranslation("dashboard.meetups-current-user-is-owner")));
        Span helperSpan =
            new Span(getTranslation("dashboard.meetups-current-user-is-owner.helper"));
        helperSpan.getStyle().set("font-size", "var(--lumo-font-size-s)");
        add(helperSpan);

        for (MeetupEvent meetup : meetupsFromCurrentUser) {
          add(buildMeetupCard(meetup));
        }
      }
      if (!meetupsFromCurrentUser.isEmpty() && !upcomingMeetups.isEmpty()) add(new Hr());
      if (!upcomingMeetups.isEmpty()) {
        add(new H3(getTranslation("dashboard.meetups-list-public")));
        for (MeetupEvent meetup : upcomingMeetups) {
          add(buildMeetupCard(meetup));
        }
      }
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

    // ── Row 1: Title ──────────────────────────────────────────────────────────
    Span title = new Span(meetup.getTitle());
    title
        .getStyle()
        .set("font-size", "var(--lumo-font-size-l)")
        .set("font-weight", "bold")
        .set("display", "block")
        .set("margin-bottom", "var(--lumo-space-xs)");

    // ── Row 2: Date ───────────────────────────────────────────────────────────
    ZonedDateTime eventDate = meetup.getEventDate();
    String dateStr = eventDate.format(localeService.getDateFormatter());
    HorizontalLayout dateRow = buildIconRow(VaadinIcon.CALENDAR, dateStr);

    // ── Row 3: Time + Duration ────────────────────────────────────────────────
    String timeStr = eventDate.format(localeService.getTimeFormatter());
    HorizontalLayout timeRow = new GameTimeAndDuration(timeStr, meetup.getDurationHours());

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

    Icon slotsIcon = VaadinIcon.USERS.create();
    slotsIcon.setSize("var(--lumo-icon-size-s)");
    slotsIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
    Span slotsSpan = new Span(slotsText);

    // Each icon+label pair is its own flex item, so they wrap together as a unit
    HorizontalLayout creatorGroup = new HorizontalLayout(personIcon, creatorSpan);
    creatorGroup.setSpacing(true);
    creatorGroup.setAlignItems(Alignment.CENTER);
    creatorGroup.getStyle().set("flex", "1 1 0").set("min-width", "140px");

    HorizontalLayout slotsGroup = new HorizontalLayout(slotsIcon, slotsSpan);
    slotsGroup.setSpacing(true);
    slotsGroup.setAlignItems(Alignment.CENTER);
    slotsGroup.getStyle().set("flex", "1 1 0").set("min-width", "140px");

    HorizontalLayout bottomRow = new HorizontalLayout(creatorGroup, slotsGroup);
    bottomRow.setSpacing(true);
    bottomRow.setAlignItems(Alignment.CENTER);
    bottomRow.getStyle().setFlexWrap(Style.FlexWrap.WRAP);

    // ── Divider ───────────────────────────────────────────────────────────────
    Div divider = new Div();
    divider
        .getStyle()
        .set("border-top", "1px solid var(--lumo-contrast-10pct)")
        .set("margin", "var(--lumo-space-xs) 0");

    card.add(title, dateRow, divider, timeRow, bottomRow);

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
    buildUI(userOpt.get().getId());
  }
}

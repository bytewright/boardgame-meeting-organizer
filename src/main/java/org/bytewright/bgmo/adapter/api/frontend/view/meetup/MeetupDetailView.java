package org.bytewright.bgmo.adapter.api.frontend.view.meetup;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.api.frontend.SessionAuthenticationService;
import org.bytewright.bgmo.adapter.api.frontend.service.i18n.LocaleService;
import org.bytewright.bgmo.adapter.api.frontend.view.DashboardView;
import org.bytewright.bgmo.adapter.api.frontend.view.component.GameTimeAndDuration;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.domain.model.Game;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.domain.model.RequestState;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.GameDao;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.usecases.MeetupWorkflows;

/**
 * Public detail page for a single meetup.
 *
 * <p>Access model:
 *
 * <ul>
 *   <li><b>Anonymous visitor</b>: sees event info and confirmed attendee names; can send a join
 *       request by supplying a display name and contact info (GDPR-consented).
 *   <li><b>Registered attendee / guest</b>: same public view plus their own request status.
 *   <li><b>Organiser (creator)</b>: full view with all join requests, contact info for anonymous
 *       requesters, and confirm/random-confirm controls.
 * </ul>
 *
 * <p>GDPR notes:
 *
 * <ul>
 *   <li>Confirmed attendee <em>names</em> are shown publicly; requesters consent to this explicitly
 *       in {@link AnonJoinDialog}.
 *   <li>Contact info is rendered <em>only</em> inside {@link #buildOwnerSection()} — it never
 *       appears in any other branch of the UI.
 *   <li>Anonymous session tokens ({@code anonToken}) are stored in the Vaadin session under a
 *       meetup-scoped key; they are not persisted beyond the session lifetime.
 * </ul>
 */
@Slf4j
@Route(value = "meetup/:meetupId", layout = MainLayout.class)
@PageTitle("Meetup Detail | " + APP_NAME_SHORT)
@AnonymousAllowed
public class MeetupDetailView extends VerticalLayout implements BeforeEnterObserver {

  /** VaadinSession attribute key prefix for the anonymous token per meetup. */
  private static final String ANON_TOKEN_KEY_PREFIX = "anonToken:meetup:";

  private final LocaleService localeService;
  private final SessionAuthenticationService authService;
  private final MeetupWorkflows meetupWorkflows;
  private final MeetupDao meetupDao;
  private final GameDao gameDao;
  private final RegisteredUserDao userDao;

  /** Null when the visitor is not logged in. */
  private RegisteredUser currentUser;

  private MeetupEvent meetup;

  public MeetupDetailView(
      LocaleService localeService,
      SessionAuthenticationService authService,
      MeetupWorkflows meetupWorkflows,
      MeetupDao meetupDao,
      GameDao gameDao,
      RegisteredUserDao userDao) {
    this.localeService = localeService;
    this.authService = authService;
    this.meetupWorkflows = meetupWorkflows;
    this.meetupDao = meetupDao;
    this.gameDao = gameDao;
    this.userDao = userDao;

    setSizeFull();
    setPadding(true);
    setSpacing(true);
    getStyle().set("max-width", MainLayout.MAX_DISPLAYPORT_WIDTH).set("margin", "0 auto");
  }

  // ── UI construction ───────────────────────────────────────────────────────

  private void buildUI() {
    removeAll();

    // ── Canceled banner ──────────────────────────────────────────────────
    if (meetup.isCanceled()) {
      Span canceledBadge = new Span(getTranslation("meetup.canceled"));
      canceledBadge
          .getStyle()
          .set("color", "var(--lumo-error-color)")
          .set("font-weight", "bold")
          .set("font-size", "1.1em");
      add(canceledBadge);
    }

    // ── Event info ───────────────────────────────────────────────────────
    H2 titleHeading = new H2(meetup.getTitle());

    ZonedDateTime eventDate = meetup.getEventDate();
    String timeStr = eventDate.format(localeService.getTimeFormatter());
    HorizontalLayout timeRow = new GameTimeAndDuration(timeStr, meetup.getDurationHours());
    Paragraph description =
        new Paragraph(
            meetup.getDescription() != null && !meetup.getDescription().isBlank()
                ? meetup.getDescription()
                : getTranslation("meetup.no-desc"));

    Icon calendarIcon = VaadinIcon.CALENDAR.create();
    calendarIcon.setSize("var(--lumo-icon-size-s)");
    calendarIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
    Span dateSpan = new Span(eventDate.format(localeService.getDateFormatter()));
    HorizontalLayout dateRow = new HorizontalLayout(calendarIcon, dateSpan);

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
    String creatorName =
        userDao
            .findById(meetup.getCreatorId())
            .map(RegisteredUser::getDisplayName)
            .orElseGet(() -> meetup.getCreatorId().toString());
    Span creatorSpan = new Span(creatorName);
    creatorSpan.setMinWidth(200, Unit.PIXELS);
    Icon slotsIcon = VaadinIcon.USERS.create();
    slotsIcon.setSize("var(--lumo-icon-size-s)");
    slotsIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
    Span slotsSpan = new Span(slotsText);

    HorizontalLayout creatorAndSlots =
        new HorizontalLayout(personIcon, creatorSpan, slotsIcon, slotsSpan);
    VerticalLayout infoSection =
        new VerticalLayout(titleHeading, dateRow, timeRow, creatorAndSlots, description);
    infoSection.setPadding(false);
    add(infoSection);
    add(new Hr());

    if (meetup.getOfferedGames() != null && !meetup.getOfferedGames().isEmpty()) {
      buildOfferedGamesSection();
      add(new Hr());
    }
    // ── Public: confirmed attendee names (visible to everyone) ────────────
    buildConfirmedAttendeesSection();

    add(new Hr());

    // ── Role-specific section ─────────────────────────────────────────────
    if (currentUser != null && meetup.getCreatorId().equals(currentUser.getId())) {
      buildOwnerSection();
    } else if (currentUser != null) {
      buildRegisteredGuestSection();
    } else {
      buildAnonSection();
    }
  }

  private void buildOfferedGamesSection() {
    add(new H3(getTranslation("meetup.offeredGames")));

    // Fetch the actual Game objects from the list of UUIDs
    List<Game> games = gameDao.findAllById(meetup.getOfferedGames());

    VerticalLayout gamesList = new VerticalLayout();
    gamesList.setPadding(false);
    gamesList.setSpacing(true);

    for (Game game : games) {
      HorizontalLayout gameCard = new HorizontalLayout();
      gameCard.setAlignItems(Alignment.CENTER);
      gameCard.setWidthFull();
      gameCard
          .getStyle()
          .set("background", "var(--lumo-contrast-5pct)")
          .set("border-radius", "8px")
          .set("padding", "var(--lumo-space-s)");

      // Using the artworkLink field
      Image img =
          new Image(
              game.getArtworkLink() != null ? game.getArtworkLink() : "images/default-game.png",
              "Game box");
      img.setWidth("60px");
      img.setHeight("60px");
      img.getStyle().set("object-fit", "cover").set("border-radius", "4px");

      VerticalLayout gameDetails = new VerticalLayout();
      gameDetails.setPadding(false);
      gameDetails.setSpacing(false);

      Span name = new Span(game.getName());
      name.getStyle().set("font-weight", "bold");

      Span players = new Span("👥 " + game.getMinPlayers() + " - " + game.getMaxPlayers());
      players
          .getStyle()
          .set("font-size", "0.85em")
          .set("color", "var(--lumo-secondary-text-color)");

      gameDetails.add(name, players);
      gameCard.add(img, gameDetails);

      // If there is a BGG link or rulebook in the urls list
      if (game.getBggId() != null) {
        Anchor bggLink =
            new Anchor("https://boardgamegeek.com/boardgame/" + game.getBggId(), "BGG");
        bggLink.setTarget("_blank");
        gameCard.add(bggLink);
      }

      gamesList.add(gameCard);
    }
    add(gamesList);
  }

  // ── Public section: confirmed names ───────────────────────────────────────

  /**
   * Shown to every visitor. Lists the display names of confirmed attendees. Contact info is never
   * shown here.
   */
  private void buildConfirmedAttendeesSection() {
    add(new H3(getTranslation("meetup.confirmedAttendees")));

    List<String> confirmedAttendees =
        meetup.getJoinRequests().stream()
            .filter(r -> r.getRequestState() == RequestState.ACCEPTED)
            .map(MeetupJoinRequest::getDisplayName)
            .toList();
    if (confirmedAttendees.isEmpty()) {
      add(new Span(getTranslation("meetup.confirmedAttendeesNone")));
      return;
    }
    VerticalLayout nameList = new VerticalLayout();
    nameList.setPadding(false);
    nameList.setSpacing(false);

    // Registered confirmed attendees
    confirmedAttendees.stream().map(name -> new Span("• " + name)).forEach(nameList::add);

    add(nameList);
  }

  // ── Anonymous visitor section ──────────────────────────────────────────────

  private void buildAnonSection() {
    UUID myToken = getAnonSessionToken(); // read-only here; only created on submit

    // Check if this browser session already has a pending or confirmed request
    Optional<MeetupJoinRequest> myRequest =
        meetup.getJoinRequests().stream()
            .filter(r -> myToken != null && myToken.equals(r.getAnonToken()))
            .findFirst();

    if (myRequest.isPresent()) {
      // Visitor already submitted a request in this session
      boolean confirmed =
          myRequest
              .map(MeetupJoinRequest::getRequestState)
              .map(state -> RequestState.ACCEPTED == state)
              .orElse(false);

      if (confirmed) {
        Span status = new Span(getTranslation("meetup.join-confirmed"));
        status.getStyle().set("color", "var(--lumo-success-color)").set("font-weight", "bold");
        add(status);
      } else {
        Span status = new Span(getTranslation("meetup.join-requested"));
        status.getStyle().set("color", "var(--lumo-primary-color)");
        add(status);
        Span hint = new Span(getTranslation("meetup.join-anonLoginHint"));
        hint.getStyle().set("font-size", "0.85em").set("color", "var(--lumo-secondary-text-color)");
        add(hint);
      }

      Button joinCancelRequestBtn =
          new Button(
              getTranslation("meetup.cancel-request"),
              VaadinIcon.CROP.create(),
              e -> cancelRequest(myRequest.get()));
      joinCancelRequestBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
      add(joinCancelRequestBtn);
      return;
    }

    // No existing request — show join button (opens dialog)
    boolean isFull = meetupWorkflows.isFull(meetup);

    if (meetup.isCanceled() || isFull) {
      Span label =
          new Span(
              meetup.isCanceled()
                  ? getTranslation("meetup.canceled")
                  : getTranslation("meetup.join-full"));
      label.getStyle().set("color", "var(--lumo-error-color)").set("font-weight", "bold");
      add(label);
      return;
    }

    Button joinBtn = new Button(getTranslation("meetup.join-request"));
    joinBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
    joinBtn.addClickListener(e -> openAnonJoinDialog());
    add(joinBtn);

    // Encourage login for persistent tracking
    Span loginHint = new Span(getTranslation("meetup.join-anonLoginHint"));
    loginHint
        .getStyle()
        .set("font-size", "0.82em")
        .set("color", "var(--lumo-secondary-text-color)");
    add(loginHint);
  }

  private void cancelRequest(MeetupJoinRequest meetupJoinRequest) {
    meetupWorkflows.cancelJoinRequest(meetupJoinRequest.getId());
    refreshMeetup();
  }

  private void openAnonJoinDialog() {
    new AnonJoinDialog(
            meetup.getTitle(),
            (displayName, contactInfo) -> {
              // Create or reuse the token for this session so the visitor can see
              // their pending status if they navigate back.
              UUID anonToken = getOrCreateAnonSessionToken();
              meetupWorkflows.requestToJoinAnon(
                  meetup.getId(), anonToken, displayName, contactInfo);

              Notification n =
                  Notification.show(
                      getTranslation("meetup.joinSent"), 3000, Notification.Position.TOP_CENTER);
              n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
              refreshMeetup();
            })
        .open();
  }

  // ── Registered guest section ───────────────────────────────────────────────

  private void buildRegisteredGuestSection() {
    Optional<MeetupJoinRequest> myRequest =
        meetup.getJoinRequests().stream()
            .filter(r -> currentUser.getId().equals(r.getUserId()))
            .findAny();
    boolean alreadyRequested =
        myRequest
            .map(MeetupJoinRequest::getRequestState)
            .map(state -> RequestState.OPEN == state)
            .orElse(false);
    boolean alreadyConfirmed =
        myRequest
            .map(MeetupJoinRequest::getRequestState)
            .map(state -> RequestState.ACCEPTED == state)
            .orElse(false);

    if (alreadyConfirmed) {
      Span status = new Span(getTranslation("meetup.join-confirmed"));
      status.getStyle().set("color", "var(--lumo-success-color)").set("font-weight", "bold");
      add(status);

      Button joinCancelRequestBtn =
          new Button(
              getTranslation("meetup.cancel-request"),
              VaadinIcon.CROP.create(),
              e -> cancelRequest(myRequest.get()));
      joinCancelRequestBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
      add(joinCancelRequestBtn);
    } else if (alreadyRequested) {
      Span status = new Span(getTranslation("meetup.join-requested"));
      status.getStyle().set("color", "var(--lumo-primary-color)");
      add(status);

      Button joinCancelRequestBtn =
          new Button(
              getTranslation("meetup.cancel-request"),
              VaadinIcon.CROP.create(),
              e -> cancelRequest(myRequest.get()));
      joinCancelRequestBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
      add(joinCancelRequestBtn);
    } else {
      boolean isFull = meetupWorkflows.isFull(meetup);
      Button joinBtn =
          new Button(
              isFull ? getTranslation("meetup.join-full") : getTranslation("meetup.join-request"),
              e -> {
                meetupWorkflows.requestToJoin(meetup.getId(), currentUser.getId(), null);
                Notification n =
                    Notification.show(
                        getTranslation("meetup.joinSent"), 3000, Notification.Position.TOP_CENTER);
                n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshMeetup();
              });
      joinBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
      joinBtn.setEnabled(!meetup.isCanceled() && !isFull);
      add(joinBtn);
    }
  }

  // ── Organiser section ──────────────────────────────────────────────────────

  /**
   * Visible only to the event creator. Shows all join requests with names, request status, and —
   * for anonymous requesters — their contact info.
   *
   * <p>Contact info is deliberately isolated here and never rendered elsewhere.
   */
  private void buildOwnerSection() {
    add(new H3(getTranslation("meetup.joinRequests")));

    List<MeetupJoinRequest> requests = meetup.getJoinRequests();

    if (requests.isEmpty()) {
      add(new Span(getTranslation("meetup.joinRequestsNone")));
    } else {
      Grid<MeetupJoinRequest> requestGrid = new Grid<>();

      // Display name column (public-safe value, shown to owner for management)
      requestGrid
          .addColumn(MeetupJoinRequest::getDisplayName)
          .setHeader(getTranslation("meetup.grid.user"))
          .setFlexGrow(1);

      // Contact info — shown only here, only to organiser
      requestGrid
          .addColumn(
              req -> {
                if (isAnonymous(req)) {
                  return req.getContactInfo() != null ? req.getContactInfo() : "—";
                }
                // Registered users: show their stored contact info if present,
                // otherwise fall back to their account e-mail (you may also omit this).
                return req.getContactInfo() != null
                    ? req.getContactInfo()
                    : userDao.find(req.getUserId()).map(RegisteredUser::getContactInfos).stream()
                        .flatMap(Collection::stream)
                        .map(Objects::toString)
                        .collect(Collectors.joining());
              })
          .setHeader(getTranslation("meetup.grid.contact"))
          .setFlexGrow(1);

      // Status column
      requestGrid
          .addColumn(
              req -> {
                boolean confirmed = RequestState.ACCEPTED == req.getRequestState();
                return confirmed
                    ? getTranslation("meetup.joinStatusConfirm")
                    : getTranslation("meetup.joinStatusPending");
              })
          .setHeader(getTranslation("meetup.grid.status"))
          .setAutoWidth(true);

      // Action column
      requestGrid
          .addComponentColumn(
              req -> {
                boolean isConfirmed = RequestState.ACCEPTED == req.getRequestState();

                Button confirmBtn =
                    new Button(
                        getTranslation(isConfirmed ? "meetup.joinStatusConfirm" : "meetup.confirm"),
                        e -> {
                          try {
                            meetupWorkflows.confirmAttendee(meetup.getId(), req);
                            Notification.show(
                                getTranslation("meetup.attendeeConfirmed", req.getDisplayName()),
                                2000,
                                Notification.Position.BOTTOM_START);
                            refreshMeetup();
                          } catch (IllegalArgumentException ex) {
                            Notification n =
                                Notification.show(
                                    ex.getMessage(), 4000, Notification.Position.MIDDLE);
                            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                          }
                        });
                confirmBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
                confirmBtn.setEnabled(!isConfirmed && !meetup.isCanceled());
                return confirmBtn;
              })
          .setHeader(getTranslation("meetup.grid.action"))
          .setAutoWidth(true);

      requestGrid.setItems(requests);
      add(requestGrid);
    }

    // Random attendee picker — only when slots are limited
    if (!meetup.isUnlimitedSlots()) {
      int remainingSlots =
          (int)
              (meetup.getJoinSlots()
                  - meetup.getJoinRequests().stream()
                      .map(MeetupJoinRequest::getRequestState)
                      .filter(state -> RequestState.ACCEPTED == state)
                      .count());

      if (remainingSlots > 0) {
        Button randomBtn =
            new Button(
                getTranslation("meetup.random-confirm", remainingSlots),
                e -> confirmRandomAttendees(remainingSlots));
        randomBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        randomBtn.setEnabled(!meetup.isCanceled());
        add(randomBtn);
      } else if (remainingSlots == 0) {
        Span fullLabel = new Span(getTranslation("meetup.allSlotsFilled"));
        fullLabel.getStyle().set("color", "var(--lumo-success-color)").set("font-weight", "bold");
        add(fullLabel);
      }
    }
  }

  private boolean isAnonymous(MeetupJoinRequest req) {
    return req.getUserId() == null;
  }

  private void confirmRandomAttendees(int slotsLeft) {
    // Collect unconfirmed registered requesters
    List<MeetupJoinRequest> openRequests =
        meetup.getJoinRequests().stream()
            .filter(r -> r.getRequestState() == RequestState.OPEN)
            .collect(Collectors.toCollection(ArrayList::new));

    if (openRequests.isEmpty()) {
      Notification.show(
          getTranslation("meetup.randomNoPending"), 3000, Notification.Position.TOP_CENTER);
      return;
    }

    Collections.shuffle(openRequests);
    List<MeetupJoinRequest> toConfirm =
        openRequests.subList(0, Math.min(slotsLeft, openRequests.size()));

    try {
      toConfirm.forEach(r -> meetupWorkflows.confirmAttendee(meetup.getId(), r));
      Notification n =
          Notification.show(
              getTranslation("meetup.randomConfirmed", toConfirm.size()),
              3000,
              Notification.Position.TOP_CENTER);
      n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
      refreshMeetup();
    } catch (IllegalArgumentException ex) {
      Notification n = Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
      n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }

  // ── Anonymous session token helpers ───────────────────────────────────────

  /**
   * Returns the anon token for this meetup stored in the current Vaadin session, or {@code null} if
   * none has been created yet (i.e. the visitor has not submitted a request in this session).
   */
  private UUID getAnonSessionToken() {
    return (UUID) VaadinSession.getCurrent().getAttribute(anonTokenKey());
  }

  /**
   * Returns the anon token for this meetup, creating and storing a new one if none exists. Call
   * this only when the visitor is actually submitting a request.
   */
  private UUID getOrCreateAnonSessionToken() {
    UUID existing = getAnonSessionToken();
    if (existing != null) {
      return existing;
    }
    UUID token = UUID.randomUUID();
    VaadinSession.getCurrent().setAttribute(anonTokenKey(), token);
    return token;
  }

  private String anonTokenKey() {
    return ANON_TOKEN_KEY_PREFIX + meetup.getId();
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private void refreshMeetup() {
    this.meetup = meetupDao.findOrThrow(meetup.getId());
    buildUI();
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    // Anonymous visitors are allowed — currentUser stays null.
    this.currentUser = authService.getCurrentUser().orElse(null);

    String meetupIdParam = event.getRouteParameters().get("meetupId").orElse(null);
    if (meetupIdParam == null) {
      log.info("Opened meetup but without id, forwarding to dashboard...");
      event.forwardTo(DashboardView.class);
      return;
    }
    try {
      this.meetup = meetupDao.findOrThrow(UUID.fromString(meetupIdParam));
    } catch (Exception e) {
      log.warn("Meetup not found or invalid UUID: {}", meetupIdParam);
      event.forwardTo(DashboardView.class);
      return;
    }
    buildUI();
  }
}

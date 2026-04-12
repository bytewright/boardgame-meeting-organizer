package org.bytewright.bgmo.adapter.api.frontend.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
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
import org.bytewright.bgmo.adapter.api.frontend.view.component.MeetupCreateDialog;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.bytewright.bgmo.usecases.MeetupWorkflows;

@Slf4j
@Route("")
@RouteAlias("dashboard")
@RouteAlias("home")
@PageTitle("Dashboard | Boardgame Meeting Organizer")
@PermitAll
public class DashboardView extends VerticalLayout implements BeforeEnterObserver {

  private static final DateTimeFormatter DATE_FMT =
          DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

  private final SessionAuthenticationService authService;
  private final MeetupWorkflows meetupWorkflows;
  private final MeetupDao meetupDao;
  private RegisteredUser currentUser;

  public DashboardView(
          SessionAuthenticationService authService,
          MeetupWorkflows meetupWorkflows,
          MeetupDao meetupDao) {
    this.authService = authService;
    this.meetupWorkflows = meetupWorkflows;
    this.meetupDao = meetupDao;

    setSizeFull();
    setPadding(true);
    setSpacing(true);
  }

  private void buildUI() {
    removeAll();

    // ── Header row ──────────────────────────────────────────────────────────
    H2 welcomeHeader = new H2("Welcome, " + currentUser.getName() + "!");

    Button createMeetupButton = new Button("+ New Meetup", e -> openCreateDialog());
    createMeetupButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button logoutButton =
            new Button(
                    "Logout",
                    e -> {
                      authService.logout();
                      UI.getCurrent().navigate(LoginView.class);
                    });

    HorizontalLayout headerLayout =
            new HorizontalLayout(welcomeHeader, createMeetupButton, logoutButton);
    headerLayout.setAlignItems(Alignment.BASELINE);
    headerLayout.setFlexGrow(1, welcomeHeader);
    add(headerLayout);

    // ── Upcoming meetups grid ────────────────────────────────────────────────
    List<MeetupEvent> upcomingMeetups =
            meetupDao.findAll().stream()
                    .filter(m -> !m.isCanceled())
                    .filter(m -> m.getEventDate().isAfter(ZonedDateTime.now()))
                    .sorted(Comparator.comparing(MeetupEvent::getEventDate))
                    .toList();

    if (upcomingMeetups.isEmpty()) {
      add(new Span("No upcoming meetups — be the first to create one!"));
      return;
    }

    Grid<MeetupEvent> grid = new Grid<>(MeetupEvent.class, false);
    grid.addColumn(MeetupEvent::getTitle).setHeader("Title").setFlexGrow(2);
    grid.addColumn(m -> m.getEventDate().format(DATE_FMT)).setHeader("Date & Time").setFlexGrow(1);
    grid.addColumn(m -> m.getDurationHours() + "h").setHeader("Duration").setAutoWidth(true);
    grid.addColumn(
                    m ->
                            m.isUnlimitedSlots()
                                    ? "∞"
                                    : m.getConfirmedAttendeeIds().size() + " / " + m.getJoinSlots())
            .setHeader("Slots")
            .setAutoWidth(true);
    grid.addComponentColumn(this::buildRowActions).setHeader("Actions").setAutoWidth(true);

    grid.setItems(upcomingMeetups);
    //grid.setHeightByRows(true);
    add(grid);
  }

  private HorizontalLayout buildRowActions(MeetupEvent meetup) {
    HorizontalLayout actions = new HorizontalLayout();
    actions.setSpacing(true);

    Button detailsBtn =
            new Button(
                    "Details",
                    e -> UI.getCurrent().navigate(MeetupDetailView.class, new RouteParam("meetupId", meetup.getId().toString())));
    detailsBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
    actions.add(detailsBtn);

    boolean isOwnMeetup = meetup.getCreatorId().equals(currentUser.getId());
    if (!isOwnMeetup) {
      boolean alreadyRequested =
              meetup.getJoinRequests().stream()
                      .anyMatch(r -> r.getUserId().equals(currentUser.getId()));
      boolean alreadyConfirmed = meetup.getConfirmedAttendeeIds().contains(currentUser.getId());
      boolean isFull =
              !meetup.isUnlimitedSlots()
              && meetup.getConfirmedAttendeeIds().size() >= meetup.getJoinSlots();

      Button joinBtn =
              new Button(
                      "Request to Join",
                      e -> {
                        meetupWorkflows.requestToJoin(meetup.getId(), currentUser.getId(), null);
                        Notification n =
                                Notification.show(
                                        "Join request sent!", 3000, Notification.Position.TOP_CENTER);
                        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        buildUI();
                      });
      joinBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);

      if (alreadyConfirmed) {
        joinBtn.setText("✓ Confirmed");
        joinBtn.setEnabled(false);
      } else if (alreadyRequested) {
        joinBtn.setText("Requested");
        joinBtn.setEnabled(false);
      } else if (isFull) {
        joinBtn.setText("Full");
        joinBtn.setEnabled(false);
      }
      actions.add(joinBtn);
    }

    return actions;
  }

  private void openCreateDialog() {
    new MeetupCreateDialog(currentUser, meetupWorkflows, this::buildUI).open();
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
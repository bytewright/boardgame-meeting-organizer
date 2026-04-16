package org.bytewright.bgmo.adapter.api.frontend.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.PermitAll;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.api.frontend.SessionAuthenticationService;
import org.bytewright.bgmo.adapter.api.frontend.service.i18n.LocaleService;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.domain.model.RequestState;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
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
  private RegisteredUser currentUser;

  public DashboardView(
      LocaleService localeService,
      SessionAuthenticationService authService,
      MeetupWorkflows meetupWorkflows,
      MeetupDao meetupDao) {
    this.localeService = localeService;
    this.authService = authService;
    this.meetupWorkflows = meetupWorkflows;
    this.meetupDao = meetupDao;

    setSizeFull();
    setPadding(true);
    setSpacing(true);
  }

  private void buildUI() {
    removeAll();

    // ── Upcoming meetups grid ────────────────────────────────────────────────
    List<MeetupEvent> upcomingMeetups =
        meetupDao.findAll().stream()
            .filter(m -> !m.isCanceled())
            .filter(m -> m.getEventDate().isAfter(ZonedDateTime.now()))
            .sorted(Comparator.comparing(MeetupEvent::getEventDate))
            .toList();

    if (upcomingMeetups.isEmpty()) {
      add(new Span(getTranslation("dashboard.no-meetups")));
      return;
    }

    Grid<MeetupEvent> grid = new Grid<>(MeetupEvent.class, false);
    grid.addColumn(MeetupEvent::getTitle)
        .setHeader(getTranslation("dashboard.grid.title"))
        .setFlexGrow(2);
    grid.addColumn(m -> m.getEventDate().format(localeService.getFormatter()))
        .setHeader(getTranslation("dashboard.grid.date"))
        .setFlexGrow(1);
    grid.addColumn(m -> getTranslation("meetup.duration", m.getDurationHours()))
        .setHeader(getTranslation("dashboard.grid.duration"))
        .setAutoWidth(true);
    grid.addColumn(
            m ->
                m.isUnlimitedSlots()
                    ? getTranslation("meetup.unlimitedSlots")
                    : getTranslation(
                        "meetup.slotsFilled",
                        m.getJoinRequests().stream()
                            .filter(
                                meetupJoinRequest ->
                                    RequestState.ACCEPTED == meetupJoinRequest.getRequestState())
                            .count(),
                        m.getJoinSlots()))
        .setHeader(getTranslation("dashboard.grid.slots"))
        .setAutoWidth(true);
    grid.addComponentColumn(this::buildRowActions)
        .setHeader(getTranslation("dashboard.grid.actions"))
        .setAutoWidth(true);
    grid.addItemClickListener(
        event ->
            UI.getCurrent()
                .navigate(
                    MeetupDetailView.class,
                    new RouteParam("meetupId", event.getItem().getId().toString())));
    grid.setItems(upcomingMeetups);
    add(grid);
  }

  private HorizontalLayout buildRowActions(MeetupEvent meetup) {
    HorizontalLayout actions = new HorizontalLayout();
    actions.setSpacing(true);

    Button detailsBtn =
        new Button(
            getTranslation("meetup.details"),
            e ->
                UI.getCurrent()
                    .navigate(
                        MeetupDetailView.class,
                        new RouteParam("meetupId", meetup.getId().toString())));
    detailsBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
    actions.add(detailsBtn);

    boolean isOwnMeetup = meetup.getCreatorId().equals(currentUser.getId());
    if (!isOwnMeetup) {
      Optional<MeetupJoinRequest> myRequest =
          meetup.getJoinRequests().stream()
              .filter(r -> currentUser.getId().equals(r.getUserId()))
              .findAny();
      boolean alreadyRequested = myRequest.isPresent();
      boolean alreadyConfirmed =
          myRequest
              .map(MeetupJoinRequest::getRequestState)
              .map(state -> RequestState.ACCEPTED == state)
              .orElse(false);
      boolean isFull = meetupWorkflows.isFull(meetup);

      Button joinBtn =
          new Button(
              getTranslation("meetup.join-request"),
              e -> {
                meetupWorkflows.requestToJoin(meetup.getId(), currentUser.getId(), null);
                Notification n =
                    Notification.show(
                        getTranslation("meetup.joinSent"), 3000, Notification.Position.TOP_CENTER);
                n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                buildUI();
              });
      joinBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);

      if (alreadyConfirmed) {
        joinBtn.setText(getTranslation("meetup.join-short.confirmed"));
        joinBtn.setEnabled(false);
      } else if (alreadyRequested) {
        joinBtn.setText(getTranslation("meetup.join-short.requested"));
        joinBtn.setEnabled(false);
      } else if (isFull) {
        joinBtn.setText(getTranslation("meetup.join-short.full"));
        joinBtn.setEnabled(false);
      }
      actions.add(joinBtn);
    }

    return actions;
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

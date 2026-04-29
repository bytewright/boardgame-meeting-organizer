package org.bytewright.bgmo.adapter.api.frontend.view.meetup;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.api.frontend.SessionAuthenticationService;
import org.bytewright.bgmo.adapter.api.frontend.view.DashboardView;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.domain.model.RequestState;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.usecases.MeetupWorkflows;

/**
 * Organiser-only view for managing join requests for a specific meetup.
 *
 * <p>Shows all requests (pending and accepted) with display names, contact info, status, and
 * confirm actions. Also provides the random-attendee-pick button.
 *
 * <p>Access is guarded in {@link #beforeEnter}: redirects to the dashboard if the current user is
 * not the meetup creator.
 */
@Slf4j
@Route(value = "meetup/:meetupId/attendees", layout = MainLayout.class)
@PageTitle("Attendees | " + APP_NAME_SHORT)
@AnonymousAllowed // fine-grained check done in beforeEnter
public class MeetupAttendeesView extends VerticalLayout implements BeforeEnterObserver {

  private final SessionAuthenticationService authService;
  private final MeetupWorkflows meetupWorkflows;
  private final MeetupDao meetupDao;
  private final RegisteredUserDao userDao;

  private MeetupEvent meetup;

  public MeetupAttendeesView(
      SessionAuthenticationService authService,
      MeetupWorkflows meetupWorkflows,
      MeetupDao meetupDao,
      RegisteredUserDao userDao) {
    this.authService = authService;
    this.meetupWorkflows = meetupWorkflows;
    this.meetupDao = meetupDao;
    this.userDao = userDao;

    setSizeFull();
    setPadding(true);
    setSpacing(true);
    getStyle().set("max-width", MainLayout.MAX_DISPLAYPORT_WIDTH).set("margin", "0 auto");
  }

  // ── UI construction ───────────────────────────────────────────────────────

  private void buildUI() {
    removeAll();

    // ── Back navigation ───────────────────────────────────────────────────────
    Button backBtn =
        new Button(
            getTranslation("meetup.attendees.back"),
            VaadinIcon.ARROW_LEFT.create(),
            e -> UI.getCurrent().navigate("meetup/" + meetup.getId()));
    backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    add(backBtn);

    add(new H2(getTranslation("meetup.attendees.title", meetup.getTitle())));

    List<MeetupJoinRequest> requests = meetup.getJoinRequests();

    if (requests.isEmpty()) {
      add(new Span(getTranslation("meetup.joinRequestsNone")));
    } else {
      add(buildRequestsGrid(requests));
    }

    // ── Random picker ─────────────────────────────────────────────────────────
    if (!meetup.isUnlimitedSlots()) {
      long accepted =
          requests.stream().filter(r -> r.getRequestState() == RequestState.ACCEPTED).count();
      int remainingSlots = (int) (meetup.getJoinSlots() - accepted);

      if (remainingSlots > 0) {
        Button randomBtn =
            new Button(
                getTranslation("meetup.random-confirm", remainingSlots),
                this::confirmRandomAttendees);
        randomBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        randomBtn.setEnabled(!meetup.isCanceled());
        add(randomBtn);
      } else {
        Span fullLabel = new Span(getTranslation("meetup.allSlotsFilled"));
        fullLabel.getStyle().set("color", "var(--lumo-success-color)").set("font-weight", "bold");
        add(fullLabel);
      }
    }
  }

  private Grid<MeetupJoinRequest> buildRequestsGrid(List<MeetupJoinRequest> requests) {
    Grid<MeetupJoinRequest> grid = new Grid<>();

    grid.addColumn(MeetupJoinRequest::getDisplayName)
        .setHeader(getTranslation("meetup.grid.user"))
        .setFlexGrow(1);

    // Contact info — organiser-only, shown only in this view
    grid.addColumn(
            req -> {
              if (req.getUserId() == null) {
                // Anonymous requester — use supplied contact string
                return req.getContactInfo() != null ? req.getContactInfo() : "—";
              }
              // Registered user — prefer any supplied note, fall back to stored contact infos
              if (req.getContactInfo() != null) {
                return req.getContactInfo();
              }
              return userDao.findById(req.getUserId()).map(RegisteredUser::getContactInfos).stream()
                  .flatMap(Collection::stream)
                  .map(Objects::toString)
                  .collect(Collectors.joining(", "));
            })
        .setHeader(getTranslation("meetup.grid.contact"))
        .setFlexGrow(1);

    grid.addColumn(
            req ->
                switch (req.getRequestState()) {
                  case OPEN -> getTranslation("meetup.joinStatusPending");
                  case ACCEPTED -> getTranslation("meetup.joinStatusConfirm");
                  case DECLINED -> getTranslation("meetup.joinStatusDeclined");
                  case CANCELED -> getTranslation("meetup.joinStatusUserCanceled");
                })
        .setHeader(getTranslation("meetup.grid.status"))
        .setAutoWidth(true);

    grid.addComponentColumn(
            req -> {
              Button btn =
                  new Button(
                      getTranslation("meetup.confirm"),
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
              btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
              boolean isConfirmed = req.getRequestState() == RequestState.ACCEPTED;
              boolean requestIsCanceled = req.getRequestState() == RequestState.CANCELED;
              btn.setEnabled(!isConfirmed && !meetup.isCanceled() && !requestIsCanceled);
              return btn;
            })
        .setHeader(getTranslation("meetup.grid.action"))
        .setAutoWidth(true);

    grid.setItems(requests);
    return grid;
  }

  private void confirmRandomAttendees(ClickEvent<Button> e) {
    try {
      int slotsFilled = meetupWorkflows.confirmRemainingSlotsRandom(meetup.id());
      Notification n =
          Notification.show(
              getTranslation("meetup.randomConfirmed", slotsFilled),
              3000,
              Notification.Position.TOP_CENTER);
      n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
      refreshMeetup();
    } catch (IllegalArgumentException ex) {
      Notification n = Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
      n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }

  private void refreshMeetup() {
    this.meetup = meetupDao.findOrThrow(meetup.getId());
    buildUI();
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    RegisteredUser currentUser = authService.getCurrentUser().orElse(null);
    if (currentUser == null) {
      event.forwardTo(DashboardView.class);
      return;
    }

    String meetupIdParam = event.getRouteParameters().get("meetupId").orElse(null);
    if (meetupIdParam == null) {
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

    if (!meetup.getCreatorId().equals(currentUser.getId())) {
      log.warn(
          "Non-organiser {} attempted to access attendees view for meetup {}",
          currentUser.logEntity(),
          meetup.logIdentity());
      event.forwardTo(DashboardView.class);
      return;
    }

    buildUI();
  }
}

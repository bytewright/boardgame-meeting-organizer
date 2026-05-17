package org.bytewright.bgmo.adapter.api.frontend.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.api.frontend.service.SessionInfoService;
import org.bytewright.bgmo.adapter.api.frontend.service.i18n.LocaleService;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MeetupBasicsInfo;
import org.bytewright.bgmo.adapter.api.frontend.view.meetup.MeetupDetailView;
import org.bytewright.bgmo.domain.model.MeetupEvent;
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
    RegisteredUser creator = registeredUserDao.findOrThrow(meetup.getCreatorId());
    MeetupBasicsInfo card = new MeetupBasicsInfo(localeService);
    MeetupBasicsInfo.Context context =
        MeetupBasicsInfo.Context.builder()
            .meetup(meetup)
            .creator(creator)
            .changeCursor(true)
            .build();
    card.buildComponent(context);
    // Hover highlight using JS — lightweight approach with style toggle
    card.getElement()
        .addEventListener(
            "mouseover", e -> card.getStyle().set("background-color", "var(--lumo-contrast-5pct)"));
    card.getElement().addEventListener("mouseout", e -> card.getStyle().remove("background-color"));

    card.addClickListener(
        e ->
            UI.getCurrent()
                .navigate(
                    MeetupDetailView.class, new RouteParam("meetupId", meetup.getId().toString())));

    return card;
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

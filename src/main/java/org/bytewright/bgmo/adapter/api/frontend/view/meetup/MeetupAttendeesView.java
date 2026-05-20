package org.bytewright.bgmo.adapter.api.frontend.view.meetup;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.PermitAll;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.api.frontend.service.ContactInfoRenderer;
import org.bytewright.bgmo.adapter.api.frontend.service.SessionInfoService;
import org.bytewright.bgmo.adapter.api.frontend.view.DashboardView;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.adapter.api.frontend.view.component.factory.ComponentFactory;
import org.bytewright.bgmo.adapter.api.frontend.view.meetup.model.MeetupAttendeesContext;
import org.bytewright.bgmo.adapter.api.frontend.view.meetup.model.MeetupAttendeesContext.AttendeeRequestItem;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.domain.model.RequestState;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.bytewright.bgmo.usecases.MeetupWorkflows;

/**
 * Organiser/admin view for managing join requests for a specific meetup.
 *
 * <p>Admins additionally see <em>Revoke</em> and <em>Delete</em> buttons for debugging and
 * data-correction purposes. These are intentionally not shown to regular organisers.
 *
 * <p>Access is guarded in {@link #beforeEnter}: non-organiser, non-admin users are forwarded to the
 * dashboard.
 */
@Slf4j
@Route(value = "meetup/:meetupId/attendees", layout = MainLayout.class)
@RouteAlias(value = "meetup/:meetupId/teilnehmer", layout = MainLayout.class)
@PageTitle("Attendees | " + APP_NAME_SHORT)
@PermitAll // fine-grained access check is done in beforeEnter
@RequiredArgsConstructor
public class MeetupAttendeesView extends VerticalLayout implements BeforeEnterObserver {
  private final ContactInfoRenderer contactInfoRenderer;
  private final ComponentFactory componentFactory;
  private final SessionInfoService authService;
  private final MeetupWorkflows meetupWorkflows;
  private final MeetupDao meetupDao;

  private MeetupEvent meetup;
  private MeetupAttendeesContext ctx;

  /** Persisted across rebuilds so the toggle survives a refresh. */
  private boolean showCanceled = false;

  private void buildUI() {
    removeAll();
    setWidthFull();
    setPadding(true);
    setSpacing(true);
    setMaxWidth(MainLayout.MAX_DISPLAYPORT_WIDTH);
    getStyle().set("margin", "0 auto");

    Button backBtn =
        new Button(
            getTranslation("meetup.attendees.back"),
            VaadinIcon.ARROW_LEFT.create(),
            e -> UI.getCurrent().navigate("meetup/" + meetup.getId()));
    backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    add(backBtn);

    add(new H2(getTranslation("meetup.attendees.title", meetup.getTitle())));

    Checkbox showCanceledToggle =
        new Checkbox(getTranslation("meetup.attendees.showCanceled"), showCanceled);
    showCanceledToggle.addValueChangeListener(
        e -> {
          showCanceled = e.getValue();
          buildUI();
        });
    add(showCanceledToggle);

    List<AttendeeRequestItem> visible =
        ctx.requests().stream()
            .filter(
                item ->
                    showCanceled
                        || (item.request().getRequestState() != RequestState.CANCELED
                            && item.request().getRequestState() != RequestState.DECLINED))
            .toList();

    if (visible.isEmpty()) {
      add(new Span(getTranslation("meetup.joinRequestsNone")));
    } else {
      VerticalLayout cardList = new VerticalLayout();
      cardList.setPadding(false);
      cardList.setSpacing(true);
      cardList.setWidthFull();
      visible.stream()
          .map(item -> componentFactory.attendeeRequestCard(ctx, item, this::buildUI))
          .forEach(cardList::add);
      add(cardList);
    }

    if (!meetup.isUnlimitedSlots()) {
      var collect =
          ctx.requests().stream()
              .collect(Collectors.groupingBy(item -> item.request().getRequestState()));
      int remainingSlots =
          meetup.getJoinSlots() - collect.getOrDefault(RequestState.ACCEPTED, List.of()).size();
      int remainingOpen = collect.getOrDefault(RequestState.OPEN, List.of()).size();
      if (remainingSlots > 0) {
        if (remainingOpen > 0) {
          Button randomBtn =
              new Button(
                  getTranslation("meetup.random-confirm", remainingSlots),
                  e -> confirmRandomAttendees());
          randomBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
          randomBtn.setEnabled(!meetup.isCanceled());
          add(randomBtn);
        }
      } else {
        Span fullLabel = new Span(getTranslation("meetup.allSlotsFilled"));
        fullLabel.getStyle().set("color", "var(--lumo-success-color)").set("font-weight", "bold");
        add(fullLabel);
      }
    }

    add(buildLegend());
  }

  private HorizontalLayout buildLegend() {
    HorizontalLayout legend = new HorizontalLayout();
    legend
        .getStyle()
        .set("margin-top", "var(--lumo-space-l)")
        .set("flex-wrap", "wrap")
        .set("color", "var(--lumo-secondary-text-color)")
        .set("font-size", "var(--lumo-font-size-s)");

    legend.add(
        legendEntry("var(--lumo-warning-color)", getTranslation("meetup.joinStatusPending")));
    legend.add(
        legendEntry("var(--lumo-success-color)", getTranslation("meetup.joinStatusConfirm")));
    legend.add(
        legendEntry(
            "var(--lumo-contrast-40pct)",
            getTranslation("meetup.attendees.legend.canceledDeclined")));
    return legend;
  }

  private HorizontalLayout legendEntry(String color, String label) {
    Span dot = new Span();
    dot.getStyle()
        .set("display", "inline-block")
        .set("width", "10px")
        .set("height", "10px")
        .set("border-radius", "50%")
        .set("background-color", color)
        .set("flex-shrink", "0");
    HorizontalLayout entry = new HorizontalLayout(dot, new Span(label));
    entry.setAlignItems(FlexComponent.Alignment.CENTER);
    entry.setSpacing(true);
    return entry;
  }

  private void confirmRandomAttendees() {
    try {
      int slotsFilled = meetupWorkflows.confirmRemainingSlotsRandom(meetup.getId());
      Notification n =
          Notification.show(
              getTranslation("meetup.randomConfirmed", slotsFilled),
              3000,
              Notification.Position.TOP_CENTER);
      n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
      refresh();
    } catch (Exception ex) {
      showError(ex.getMessage());
    }
  }

  private void refresh() {
    this.meetup = meetupDao.findOrThrow(meetup.getId());
    this.ctx = build(meetup);
    buildUI();
  }

  public MeetupAttendeesContext build(MeetupEvent meetup) {
    var items =
        meetup.getJoinRequests().stream()
            .sorted(Comparator.comparing(MeetupJoinRequest::getTsCreation))
            .map(req -> new AttendeeRequestItem(req, contactInfoRenderer.render(req)))
            .toList();
    return new MeetupAttendeesContext(meetup.id(), meetup.isCanceled(), items);
  }

  private void showError(String message) {
    Notification n = Notification.show(message, 4000, Notification.Position.MIDDLE);
    n.addThemeVariants(NotificationVariant.LUMO_ERROR);
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

    if (!authService.isCurrentUserAdmin() && !meetup.getCreatorId().equals(currentUser.getId())) {
      log.warn(
          "Non-organiser and non-admin {} attempted to access attendees view for meetup {}",
          currentUser.logEntity(),
          meetup.logIdentity());
      event.forwardTo(DashboardView.class);
      return;
    }

    this.ctx = build(meetup);
    buildUI();
  }
}

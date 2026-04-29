package org.bytewright.bgmo.adapter.api.frontend.view.meetup.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouteParam;
import org.bytewright.bgmo.adapter.api.frontend.service.MeetupDetailContext;
import org.bytewright.bgmo.adapter.api.frontend.view.component.NavCardComponent;
import org.bytewright.bgmo.adapter.api.frontend.view.meetup.MeetupAttendeesView;
import org.bytewright.bgmo.adapter.api.frontend.view.meetup.dialog.CancelEventDialog;
import org.bytewright.bgmo.adapter.api.frontend.view.meetup.dialog.RescheduleDialog;
import org.bytewright.bgmo.usecases.MeetupWorkflows;

/**
 * Organiser-only panel rendered as a navigation card hub.
 *
 * <p>Each card leads to either a dedicated view or opens a focused dialog. Heavy domain work
 * (grids, forms) lives in those targets, keeping this panel thin.
 *
 * <p>Card layout:
 *
 * <ul>
 *   <li>Attendee Management → {@link MeetupAttendeesView} (highlighted when pending requests exist)
 *   <li>Reschedule → {@link RescheduleDialog}
 *   <li>Cancel Event → {@link CancelEventDialog} (hidden when already canceled)
 * </ul>
 */
public class OrganizerPanel extends VerticalLayout {

  public OrganizerPanel(
      MeetupDetailContext ctx, MeetupWorkflows meetupWorkflows, Runnable onRefresh) {

    setPadding(false);
    setSpacing(true);

    add(new H3(getTranslation("meetup.organizer.title")));

    FlexLayout cards = new FlexLayout();
    cards.setFlexWrap(FlexLayout.FlexWrap.WRAP);
    cards.getStyle().set("gap", "var(--lumo-space-m)");

    // ── Attendee management ──────────────────────────────────────────────────
    long pendingCount = ctx.pendingRequestCount();
    String attendeesSubtitle =
        pendingCount > 0
            ? getTranslation("meetup.organizer.manageAttendees.pending", pendingCount)
            : getTranslation("meetup.organizer.manageAttendees.subtitle");

    cards.add(
        new NavCardComponent(
            VaadinIcon.USERS,
            getTranslation("meetup.organizer.manageAttendees"),
            attendeesSubtitle,
            pendingCount > 0,
            () ->
                UI.getCurrent()
                    .navigate(
                        MeetupAttendeesView.class,
                        new RouteParam("meetupId", ctx.meetup().id().toString()))));

    // ── Reschedule ───────────────────────────────────────────────────────────
    cards.add(
        new NavCardComponent(
            VaadinIcon.CALENDAR_CLOCK,
            getTranslation("meetup.organizer.reschedule"),
            getTranslation("meetup.organizer.reschedule.subtitle"),
            false,
            () -> new RescheduleDialog(ctx.meetup(), meetupWorkflows, onRefresh).open()));

    // ── Cancel event (hidden when already canceled) ──────────────────────────
    if (!ctx.meetup().isCanceled()) {
      cards.add(
          new NavCardComponent(
              VaadinIcon.BAN,
              getTranslation("meetup.organizer.cancel"),
              getTranslation("meetup.organizer.cancel.subtitle"),
              false,
              () -> new CancelEventDialog(ctx.meetup(), meetupWorkflows, onRefresh).open()));
    }

    add(cards);
  }
}

package org.bytewright.bgmo.adapter.api.frontend.view.meetup.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.time.ZonedDateTime;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.RequestState;
import org.bytewright.bgmo.usecases.MeetupWorkflows;

/**
 * Dialog for rescheduling a meetup event to a new date and time.
 *
 * <p>Displays a warning showing how many attendees will be notified. Calls {@link
 * MeetupWorkflows#rescheduleEvent(java.util.UUID, ZonedDateTime, ZonedDateTime)} on confirmation;
 * that method is responsible for dispatching notifications.
 *
 * <p>The pickers work in the event's original timezone to avoid accidental shifts.
 */
public class RescheduleDialog extends Dialog {

  public RescheduleDialog(MeetupEvent meetup, MeetupWorkflows meetupWorkflows, Runnable onSuccess) {
    setHeaderTitle(getTranslation("meetup.reschedule.dialog.title"));
    setWidth("400px");

    // ── Notification warning ──────────────────────────────────────────────────
    long affectedCount =
        meetup.getJoinRequests().stream()
            .filter(
                r ->
                    r.getRequestState() == RequestState.OPEN
                        || r.getRequestState() == RequestState.ACCEPTED)
            .filter(r -> r.getUserId() != null) // only registered users receive bot notifications
            .count();

    Paragraph warning = new Paragraph(getTranslation("meetup.reschedule.warn", affectedCount));
    warning
        .getStyle()
        .set("color", "var(--lumo-warning-text-color)")
        .set("font-size", "var(--lumo-font-size-s)");

    // ── New event date picker ─────────────────────────────────────────────────
    DateTimePicker newDatePicker = new DateTimePicker(getTranslation("meetup.reschedule.newDate"));
    newDatePicker.setValue(meetup.getEventDate().toLocalDateTime());
    newDatePicker.setWidthFull();

    // ── New registration close picker ─────────────────────────────────────────
    DateTimePicker newRegClosePicker =
        new DateTimePicker(getTranslation("meetup.reschedule.newRegistrationClose"));
    newRegClosePicker.setValue(meetup.getRegistrationClosing().toLocalDateTime());
    newRegClosePicker.setWidthFull();

    VerticalLayout content =
        new VerticalLayout(
            new H3(getTranslation("meetup.reschedule.dialog.title")),
            warning,
            newDatePicker,
            newRegClosePicker);
    content.setPadding(false);
    content.setSpacing(true);
    add(content);

    // ── Buttons ───────────────────────────────────────────────────────────────
    Button confirm =
        new Button(
            getTranslation("meetup.reschedule.confirm"),
            e -> {
              if (newDatePicker.getValue() == null || newRegClosePicker.getValue() == null) {
                Notification.show(
                    getTranslation("meetup.reschedule.error.empty"),
                    3000,
                    Notification.Position.MIDDLE);
                return;
              }

              ZonedDateTime newDate =
                  ZonedDateTime.of(newDatePicker.getValue(), meetup.getEventDate().getZone());
              ZonedDateTime newRegClose =
                  ZonedDateTime.of(
                      newRegClosePicker.getValue(), meetup.getRegistrationClosing().getZone());

              meetupWorkflows.rescheduleEvent(meetup.getId(), newDate, newRegClose);

              Notification n =
                  Notification.show(
                      getTranslation("meetup.reschedule.success"),
                      3000,
                      Notification.Position.TOP_CENTER);
              n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
              close();
              onSuccess.run();
            });
    confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button cancel = new Button(getTranslation("action.cancel"), e -> close());
    cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    getFooter().add(new HorizontalLayout(cancel, confirm));
  }
}

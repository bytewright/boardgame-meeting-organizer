package org.bytewright.bgmo.adapter.api.frontend.view.meetup.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.RequestState;
import org.bytewright.bgmo.usecases.MeetupWorkflows;

/**
 * Confirmation dialog for canceling a meetup event.
 *
 * <p>Shows how many attendees have active join requests so the organiser understands the impact
 * before confirming. The actual cancellation and attendee notification is handled by the use case
 * layer.
 */
public class CancelEventDialog extends Dialog {

  public CancelEventDialog(
      MeetupEvent meetup, MeetupWorkflows meetupWorkflows, Runnable onSuccess) {

    setHeaderTitle(getTranslation("meetup.cancel.dialog.title"));

    long affectedCount =
        meetup.getJoinRequests().stream()
            .filter(
                r ->
                    r.getRequestState() == RequestState.OPEN
                        || r.getRequestState() == RequestState.ACCEPTED)
            .count();

    Paragraph warning =
        new Paragraph(
            getTranslation("meetup.cancel.dialog.warn", meetup.getTitle(), affectedCount));
    warning.getStyle().set("color", "var(--lumo-error-text-color)");
    add(warning);

    Button confirm =
        new Button(
            getTranslation("meetup.cancel.confirm"),
            e -> {
              meetupWorkflows.cancelMeetup(meetup.getId());
              Notification n =
                  Notification.show(
                      getTranslation("meetup.cancel.success"),
                      3000,
                      Notification.Position.TOP_CENTER);
              n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
              close();
              onSuccess.run();
            });
    confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

    Button cancel = new Button(getTranslation("action.cancel"), e -> close());
    cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    getFooter().add(new HorizontalLayout(cancel, confirm));
  }
}

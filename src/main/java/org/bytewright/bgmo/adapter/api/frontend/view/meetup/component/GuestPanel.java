package org.bytewright.bgmo.adapter.api.frontend.view.meetup.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.bytewright.bgmo.adapter.api.frontend.service.MeetupDetailContext;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.domain.model.RequestState;
import org.bytewright.bgmo.usecases.MeetupWorkflows;

/**
 * Role panel for logged-in non-organiser users.
 *
 * <p>Handles two roles that share the same join/cancel actions:
 *
 * <ul>
 *   <li>{@code REGISTERED_PENDING} — either no request yet (show join button) or an OPEN request
 *       (show status + cancel button).
 *   <li>{@code REGISTERED_ACCEPTED} — confirmed; also shows the address hint (the full address is
 *       rendered in {@link MeetupInfoHeader}).
 * </ul>
 */
public class GuestPanel extends VerticalLayout {

  public GuestPanel(MeetupDetailContext ctx, MeetupWorkflows meetupWorkflows, Runnable onRefresh) {
    setPadding(false);
    setSpacing(true);

    switch (ctx.role()) {
      case REGISTERED_ACCEPTED -> buildAccepted(ctx, meetupWorkflows, onRefresh);
      case REGISTERED_CANCELED -> buildCanceled(ctx, meetupWorkflows, onRefresh);
      default -> buildPendingOrNone(ctx, meetupWorkflows, onRefresh);
    }
  }

  private void buildCanceled(
      MeetupDetailContext ctx, MeetupWorkflows meetupWorkflows, Runnable onRefresh) {
    Span status = new Span(getTranslation("meetup.join-canceled"));
    status.getStyle().set("color", "var(--lumo-success-color)").set("font-weight", "bold");
    add(status);
    buildPendingOrNone(ctx, meetupWorkflows, onRefresh);
  }

  // ── Accepted ────────────────────────────────────────────────────────────────

  private void buildAccepted(
      MeetupDetailContext ctx, MeetupWorkflows meetupWorkflows, Runnable onRefresh) {

    Span status = new Span(getTranslation("meetup.join-confirmed"));
    status.getStyle().set("color", "var(--lumo-success-color)").set("font-weight", "bold");
    add(status);

    Span addressHint = new Span(getTranslation("meetup.join-accepted.address-hint"));
    addressHint
        .getStyle()
        .set("font-size", "var(--lumo-font-size-s)")
        .set("color", "var(--lumo-secondary-text-color)");
    add(addressHint);

    add(buildCancelButton(ctx.myRequest().orElseThrow(), meetupWorkflows, onRefresh));
  }

  // ── Pending or no request ───────────────────────────────────────────────────

  private void buildPendingOrNone(
      MeetupDetailContext ctx, MeetupWorkflows meetupWorkflows, Runnable onRefresh) {

    if (ctx.myRequest().isPresent()
        && ctx.myRequest().get().getRequestState() != RequestState.CANCELED) {
      // Has an OPEN request
      Span status = new Span(getTranslation("meetup.join-requested"));
      status.getStyle().set("color", "var(--lumo-primary-color)").set("font-weight", "bold");
      add(status);
      add(buildCancelButton(ctx.myRequest().get(), meetupWorkflows, onRefresh));
      return;
    }

    // No request yet — show join button
    if (ctx.meetup().isCanceled()) {
      addStatusLabel(getTranslation("meetup.canceled"), "var(--lumo-error-color)");
      return;
    }

    boolean isFull = ctx.isFull();
    Button joinBtn =
        new Button(
            isFull ? getTranslation("meetup.join-full") : getTranslation("meetup.join-request"),
            e -> {
              meetupWorkflows.requestToJoin(ctx.meetup().getId(), ctx.currentUser().getId(), null);
              Notification n =
                  Notification.show(
                      getTranslation("meetup.joinSent"), 3000, Notification.Position.TOP_CENTER);
              n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
              onRefresh.run();
            });
    joinBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
    joinBtn.setEnabled(!isFull);
    add(joinBtn);
  }

  // ── Helpers ──────────────────────────────────────────────────────────────────

  private Button buildCancelButton(
      MeetupJoinRequest request, MeetupWorkflows meetupWorkflows, Runnable onRefresh) {
    Button btn =
        new Button(
            getTranslation("meetup.cancel-request"),
            VaadinIcon.CLOSE.create(),
            e -> {
              meetupWorkflows.cancelJoinRequest(request.getId());
              onRefresh.run();
            });
    btn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
    return btn;
  }

  private void addStatusLabel(String text, String color) {
    Span label = new Span(text);
    label.getStyle().set("color", color).set("font-weight", "bold");
    add(label);
  }
}

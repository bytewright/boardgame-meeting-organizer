package org.bytewright.bgmo.adapter.api.frontend.view.meetup.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.UUID;
import java.util.function.Supplier;
import org.bytewright.bgmo.adapter.api.frontend.service.MeetupDetailContext;
import org.bytewright.bgmo.adapter.api.frontend.view.meetup.MeetupDetailView;
import org.bytewright.bgmo.adapter.api.frontend.view.meetup.ViewerRole;
import org.bytewright.bgmo.adapter.api.frontend.view.meetup.dialog.AnonJoinDialog;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.usecases.MeetupWorkflows;

/**
 * Role panel for anonymous visitors. Handles all three anon sub-states:
 *
 * <ul>
 *   <li>{@link ViewerRole#ANONYMOUS} — no existing request; shows the join button.
 *   <li>{@link ViewerRole#ANON_PENDING} — request submitted this session, awaiting confirmation.
 *   <li>{@link ViewerRole#ANON_ACCEPTED} — accepted; shows confirmation and full address (via
 *       {@link MeetupInfoHeader} which reads the context).
 * </ul>
 *
 * <p>The anon session token is managed by {@link MeetupDetailView} and supplied here as a {@link
 * Supplier} so this component never has to touch {@code VaadinSession} directly.
 */
public class AnonPanel extends VerticalLayout {

  public AnonPanel(
      MeetupDetailContext ctx,
      MeetupWorkflows meetupWorkflows,
      Supplier<UUID> getOrCreateAnonToken,
      Runnable onRefresh) {

    setPadding(false);
    setSpacing(true);

    switch (ctx.role()) {
      case ANON_PENDING -> buildPending(ctx, meetupWorkflows, onRefresh);
      case ANON_ACCEPTED -> buildAccepted(ctx, meetupWorkflows, onRefresh);
      default -> buildNoRequest(ctx, meetupWorkflows, getOrCreateAnonToken, onRefresh);
    }
  }

  // ── No request yet ──────────────────────────────────────────────────────────

  private void buildNoRequest(
      MeetupDetailContext ctx,
      MeetupWorkflows meetupWorkflows,
      Supplier<UUID> getOrCreateAnonToken,
      Runnable onRefresh) {

    if (ctx.meetup().isCanceled()) {
      addErrorLabel(getTranslation("meetup.canceled"));
      return;
    }
    if (ctx.isFull()) {
      addErrorLabel(getTranslation("meetup.join-full"));
      return;
    }

    Button joinBtn = new Button(getTranslation("meetup.join-request"));
    joinBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
    joinBtn.addClickListener(
        e ->
            new AnonJoinDialog(
                    ctx.meetup().getTitle(),
                    (displayName, contactInfo) -> {
                      UUID token = getOrCreateAnonToken.get();
                      meetupWorkflows.requestToJoinAnon(
                          ctx.meetup().getId(), token, displayName, contactInfo);
                      Notification n =
                          Notification.show(
                              getTranslation("meetup.joinSent"),
                              3000,
                              Notification.Position.TOP_CENTER);
                      n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                      onRefresh.run();
                    })
                .open());
    add(joinBtn);

    add(buildLoginHint());
  }

  // ── Pending (OPEN request in this session) ──────────────────────────────────

  private void buildPending(
      MeetupDetailContext ctx, MeetupWorkflows meetupWorkflows, Runnable onRefresh) {

    Span status = new Span(getTranslation("meetup.join-requested"));
    status.getStyle().set("color", "var(--lumo-primary-color)").set("font-weight", "bold");
    add(status);
    add(buildLoginHint());
    add(buildCancelButton(ctx.myRequest().orElseThrow(), meetupWorkflows, onRefresh));
  }

  // ── Accepted ────────────────────────────────────────────────────────────────

  private void buildAccepted(
      MeetupDetailContext ctx, MeetupWorkflows meetupWorkflows, Runnable onRefresh) {

    Span status = new Span(getTranslation("meetup.join-confirmed"));
    status.getStyle().set("color", "var(--lumo-success-color)").set("font-weight", "bold");
    add(status);

    // The full address is already shown by MeetupInfoHeader when showFullAddress() is true.
    // We only add a hint here so the user knows where to look.
    Span addressHint = new Span(getTranslation("meetup.join-accepted.address-hint"));
    addressHint
        .getStyle()
        .set("font-size", "var(--lumo-font-size-s)")
        .set("color", "var(--lumo-secondary-text-color)");
    add(addressHint);

    add(buildCancelButton(ctx.myRequest().orElseThrow(), meetupWorkflows, onRefresh));

    // Reiterate session-persistence warning for accepted anons
    Span sessionWarning = new Span(getTranslation("meetup.anon.session-warning"));
    sessionWarning
        .getStyle()
        .set("font-size", "var(--lumo-font-size-xs)")
        .set("color", "var(--lumo-secondary-text-color)");
    add(sessionWarning);
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

  private Span buildLoginHint() {
    Span hint = new Span(getTranslation("meetup.join-anonLoginHint"));
    hint.getStyle()
        .set("font-size", "var(--lumo-font-size-s)")
        .set("color", "var(--lumo-secondary-text-color)");
    return hint;
  }

  private void addErrorLabel(String text) {
    Span label = new Span(text);
    label.getStyle().set("color", "var(--lumo-error-color)").set("font-weight", "bold");
    add(label);
  }
}

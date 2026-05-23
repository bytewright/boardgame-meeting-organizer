package org.bytewright.bgmo.adapter.api.frontend.view.meetup.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.validator.EmailValidator;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.bytewright.bgmo.adapter.api.frontend.service.MeetupDetailContext;
import org.bytewright.bgmo.adapter.api.frontend.view.meetup.MeetupDetailView;
import org.bytewright.bgmo.adapter.api.frontend.view.meetup.ViewerRole;
import org.bytewright.bgmo.domain.model.JoinRequestPayload;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
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
 *
 * <p>The join dialog ({@link AnonJoinDialog}) is a static inner class so it can share the {@link
 * AnonJoinDialog.ContactMode} enum and stay co-located with the panel that owns the workflow
 * dispatch.
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
    }

    Button joinBtn = new Button(getTranslation("meetup.join-request"));
    joinBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
    if (ctx.meetup().isAllowAnonSignup()) {
      joinBtn.addClickListener(
          e ->
              new AnonJoinDialog(
                      ctx.meetup().getTitle(),
                      dialog ->
                          handleDialogSubmit(
                              dialog, ctx, meetupWorkflows, getOrCreateAnonToken, onRefresh))
                  .open());
      joinBtn.setDisableOnClick(true);
      add(joinBtn);
    } else {
      joinBtn.setEnabled(false);
      add(joinBtn);
      add(new Span(getTranslation("meetup.no-anon-allowed")));
    }

    add(buildLoginHint());
  }

  /**
   * Dispatches the correct workflow method based on the contact mode chosen in the dialog.
   *
   * <p>EMAIL mode produces a typed {@link JoinRequestPayload.AnonEmail} so the notification
   * adapters can reach the attendee automatically. FREEFORM falls back to the plain {@link
   * JoinRequestPayload.Anon} string payload — the organiser must contact the attendee manually.
   *
   * <p>Note: {@link JoinRequestPayload} must list {@code AnonEmail} in its {@code permits} clause.
   */
  private void handleDialogSubmit(
      AnonJoinDialog dialog,
      MeetupDetailContext ctx,
      MeetupWorkflows meetupWorkflows,
      Supplier<UUID> getOrCreateAnonToken,
      Runnable onRefresh) {

    UUID token = getOrCreateAnonToken.get();
    UUID meetupId = ctx.meetup().getId();

    switch (dialog.getContactMode()) {
      case EMAIL ->
          meetupWorkflows.requestToJoinAnon(
              meetupId,
              new JoinRequestPayload.AnonEmail(
                  dialog.getDisplayName(), token, dialog.getEmailContact()));
      case FREEFORM ->
          meetupWorkflows.requestToJoinAnon(
              meetupId,
              new JoinRequestPayload.Anon(
                  dialog.getDisplayName(), token, dialog.getFreeformContact()));
    }

    Notification.show(getTranslation("meetup.joinSent"), 3000, Notification.Position.TOP_CENTER)
        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    onRefresh.run();
  }

  private void buildPending(
      MeetupDetailContext ctx, MeetupWorkflows meetupWorkflows, Runnable onRefresh) {

    Span status = new Span(getTranslation("meetup.join-requested"));
    status.getStyle().set("color", "var(--lumo-primary-color)").set("font-weight", "bold");
    add(status);
    add(buildLoginHint());
    add(buildCancelButton(ctx.myRequest().orElseThrow(), meetupWorkflows, onRefresh));
  }

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

  public static class AnonJoinDialog extends Dialog {

    /**
     * How the anonymous attendee wants to be contacted.
     *
     * <ul>
     *   <li>{@link ContactMode#EMAIL} (default) — validated e-mail address. The application can
     *       then send automatic notifications (confirmation, reschedule, cancellation, lottery
     *       result).
     *   <li>{@link ContactMode#FREEFORM} — free-text field accepting any contact detail (e.g.
     *       phone, Signal handle). A highlighted warning explains which automatic notifications
     *       will be skipped; the organiser must contact the attendee manually.
     * </ul>
     *
     * <p>Adding new entries here (e.g. {@code TELEGRAM}) will automatically show up in the radio
     * group once wired in {@link #buildModeGroup()}.
     */
    public enum ContactMode {
      EMAIL,
      FREEFORM
    }

    private final TextField nameField;
    private final RadioButtonGroup<ContactMode> modeGroup;
    private final EmailField emailField;
    private final TextField freeformContactField;
    private final Checkbox consentBox;

    public AnonJoinDialog(String meetupTitle, Consumer<AnonJoinDialog> onSubmit) {
      setHeaderTitle(getTranslation("anon-join-dialog.header", meetupTitle));
      setCloseOnOutsideClick(false);
      setWidth("480px");

      nameField = new TextField(getTranslation("anon-join-dialog.name.label"));
      nameField.setRequired(true);
      nameField.setPlaceholder(getTranslation("anon-join-dialog.name.placeholder"));
      nameField.setWidthFull();

      modeGroup = buildModeGroup();

      emailField = new EmailField(getTranslation("anon-join-dialog.email.label"));
      emailField.setRequired(true);
      emailField.setPlaceholder(getTranslation("anon-join-dialog.email.placeholder"));
      emailField.setWidthFull();

      freeformContactField = new TextField(getTranslation("anon-join-dialog.contact.label"));
      freeformContactField.setRequired(true);
      freeformContactField.setPlaceholder(getTranslation("anon-join-dialog.contact.placeholder"));
      freeformContactField.setHelperText(getTranslation("anon-join-dialog.contact.helper"));
      freeformContactField.setWidthFull();
      freeformContactField.setVisible(false);

      Div warningDiv = buildWarningDiv();
      warningDiv.setVisible(false);

      modeGroup.addValueChangeListener(
          e -> {
            boolean isFreeform = e.getValue() == ContactMode.FREEFORM;
            emailField.setVisible(!isFreeform);
            freeformContactField.setVisible(isFreeform);
            warningDiv.setVisible(isFreeform);
          });

      Paragraph gdprNote = new Paragraph(getTranslation("anon-join-dialog.gdpr.text"));
      gdprNote
          .getStyle()
          .set("font-size", "0.82em")
          .set("color", "var(--lumo-secondary-text-color)")
          .set("line-height", "1.5");

      consentBox = new Checkbox(getTranslation("anon-join-dialog.consent.label"));

      // ── Buttons ───────────────────────────────────────────────────────────────
      Button cancelBtn = new Button(getTranslation("anon-join-dialog.btn.cancel"), e -> close());
      cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

      Button submitBtn = new Button(getTranslation("anon-join-dialog.btn.submit"));
      submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
      submitBtn.addClickListener(
          e -> {
            if (validate()) {
              onSubmit.accept(this);
              close();
            }
          });

      // ── Layout ────────────────────────────────────────────────────────────────
      VerticalLayout content =
          new VerticalLayout(
              nameField,
              modeGroup,
              emailField,
              freeformContactField,
              warningDiv,
              gdprNote,
              consentBox);
      content.setPadding(false);
      content.setSpacing(true);
      add(content);

      getFooter().add(cancelBtn, submitBtn);
    }

    private RadioButtonGroup<ContactMode> buildModeGroup() {
      RadioButtonGroup<ContactMode> group = new RadioButtonGroup<>();
      group.setLabel(getTranslation("anon-join-dialog.mode.label"));
      group.setItems(ContactMode.EMAIL, ContactMode.FREEFORM);
      group.setItemLabelGenerator(
          mode ->
              switch (mode) {
                case EMAIL -> getTranslation("anon-join-dialog.mode.email");
                case FREEFORM -> getTranslation("anon-join-dialog.mode.freeform");
              });
      group.setValue(ContactMode.EMAIL);
      return group;
    }

    private Div buildWarningDiv() {
      Div warning = new Div();
      warning
          .getStyle()
          .set("background-color", "var(--lumo-warning-color-10pct)")
          .set("border-left", "4px solid var(--lumo-warning-color)")
          .set("padding", "var(--lumo-space-m)")
          .set("border-radius", "var(--lumo-border-radius-m)")
          .set("width", "100%")
          .set("box-sizing", "border-box");

      Span title = new Span(getTranslation("anon-join-dialog.freeform.warning.title"));
      title
          .getStyle()
          .set("font-weight", "bold")
          .set("display", "block")
          .set("margin-bottom", "var(--lumo-space-xs)");

      Span text = new Span(getTranslation("anon-join-dialog.freeform.warning.text"));
      text.getStyle().set("font-size", "var(--lumo-font-size-s)");

      warning.add(title, text);
      return warning;
    }

    private boolean validate() {
      boolean valid = true;
      if (nameField.getValue() == null || nameField.getValue().isBlank()) {
        nameField.setInvalid(true);
        nameField.setErrorMessage(getTranslation("anon-join-dialog.validation.name-required"));
        valid = false;
      } else {
        nameField.setInvalid(false);
      }

      // Contact — branches on current mode
      if (modeGroup.getValue() == ContactMode.EMAIL) {
        valid = validateEmailField() && valid;
      } else {
        valid = validateFreeformField() && valid;
      }

      if (!consentBox.getValue()) {
        Notification n =
            Notification.show(
                getTranslation("anon-join-dialog.validation.consent-required"),
                3500,
                Notification.Position.MIDDLE);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        valid = false;
      }

      return valid;
    }

    private boolean validateEmailField() {
      String email = emailField.getValue();

      if (email == null || email.isBlank()) {
        emailField.setInvalid(true);
        emailField.setErrorMessage(getTranslation("anon-join-dialog.validation.email-required"));
        return false;
      }

      var result =
          new EmailValidator(getTranslation("anon-join-dialog.validation.email-invalid"))
              .apply(email, new ValueContext(emailField));

      if (result.isError()) {
        emailField.setInvalid(true);
        emailField.setErrorMessage(result.getErrorMessage());
        return false;
      }

      emailField.setInvalid(false);
      return true;
    }

    private boolean validateFreeformField() {
      if (freeformContactField.getValue() == null || freeformContactField.getValue().isBlank()) {
        freeformContactField.setInvalid(true);
        freeformContactField.setErrorMessage(
            getTranslation("anon-join-dialog.validation.contact-required"));
        return false;
      }
      freeformContactField.setInvalid(false);
      return true;
    }

    /** The trimmed display name entered by the user. */
    public String getDisplayName() {
      return nameField.getValue().trim();
    }

    /** Which contact mode was active when the form was submitted. */
    public ContactMode getContactMode() {
      return modeGroup.getValue();
    }

    /**
     * A typed {@link ContactInfo.EmailContact} built from the validated email field.
     *
     * <p>Only call this when {@link #getContactMode()} is {@link ContactMode#EMAIL}.
     */
    public ContactInfo.EmailContact getEmailContact() {
      return new ContactInfo.EmailContact(emailField.getValue().trim());
    }

    /**
     * The raw freeform contact string entered by the user.
     *
     * <p>Only call this when {@link #getContactMode()} is {@link ContactMode#FREEFORM}.
     */
    public String getFreeformContact() {
      return freeformContactField.getValue().trim();
    }
  }
}

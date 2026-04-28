package org.bytewright.bgmo.adapter.api.frontend.view.meetup;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import java.util.function.BiConsumer;

/**
 * Modal dialog that collects the minimum data required for an anonymous join request: a display
 * name (shown publicly after confirmation) and a contact detail (shared only with the organiser
 * once they confirm the attendee).
 *
 * <p>A GDPR-compliant consent notice and explicit checkbox are always shown before the form can be
 * submitted.
 */
public class AnonJoinDialog extends Dialog {

  /**
   * @param meetupTitle the event title shown in the dialog header
   * @param onSubmit called with (displayName, contactInfo) when the user confirms; the caller is
   *     responsible for persisting the request and storing the anon session token
   */
  public AnonJoinDialog(String meetupTitle, BiConsumer<String, String> onSubmit) {
    setHeaderTitle("Request to join: " + meetupTitle);
    setCloseOnOutsideClick(false);
    setWidth("480px");

    // ── Fields ────────────────────────────────────────────────────────────
    TextField nameField = new TextField("Your name");
    nameField.setRequired(true);
    nameField.setPlaceholder("How others will see you, e.g. \"Alex\"");
    nameField.setWidthFull();

    TextField contactField = new TextField("Contact info");
    contactField.setRequired(true);
    contactField.setPlaceholder("Telegram @handle · Signal/phone · e-mail …");
    contactField.setHelperText(
        "Only the organiser sees this — and only after they confirm your spot.");
    contactField.setWidthFull();

    // ── GDPR notice ───────────────────────────────────────────────────────
    Paragraph gdprNote =
        new Paragraph(
            "Data handling: your display name will be shown publicly to other attendees once "
                + "the organiser confirms your spot. Your contact info is stored securely, "
                + "is visible only to the event organiser, and will be deleted after the event "
                + "concludes. You can request early deletion by contacting the organiser directly. "
                + "No data is shared with third parties.");
    gdprNote
        .getStyle()
        .set("font-size", "0.82em")
        .set("color", "var(--lumo-secondary-text-color)")
        .set("line-height", "1.5");

    Checkbox consentBox = new Checkbox("I have read and agree to the above data handling.");

    // ── Buttons ───────────────────────────────────────────────────────────
    Button cancelBtn = new Button("Cancel", e -> close());
    cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    Button submitBtn = new Button("Send join request");
    submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
    submitBtn.addClickListener(
        e -> {
          boolean valid = true;

          if (nameField.getValue() == null || nameField.getValue().isBlank()) {
            nameField.setInvalid(true);
            nameField.setErrorMessage("Please enter a name.");
            valid = false;
          } else {
            nameField.setInvalid(false);
          }

          if (contactField.getValue() == null || contactField.getValue().isBlank()) {
            contactField.setInvalid(true);
            contactField.setErrorMessage("Please enter how the organiser can reach you.");
            valid = false;
          } else {
            contactField.setInvalid(false);
          }

          if (!consentBox.getValue()) {
            Notification n =
                Notification.show(
                    "Please accept the data handling notice before sending your request.",
                    3500,
                    Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            valid = false;
          }

          if (valid) {
            onSubmit.accept(nameField.getValue().trim(), contactField.getValue().trim());
            close();
          }
        });

    // ── Layout ────────────────────────────────────────────────────────────
    VerticalLayout content = new VerticalLayout(nameField, contactField, gdprNote, consentBox);
    content.setPadding(false);
    content.setSpacing(true);
    add(content);

    getFooter().add(cancelBtn, submitBtn);
  }
}

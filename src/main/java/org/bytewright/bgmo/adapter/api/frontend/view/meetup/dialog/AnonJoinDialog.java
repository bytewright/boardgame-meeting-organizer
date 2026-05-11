package org.bytewright.bgmo.adapter.api.frontend.view.meetup.dialog;

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
    setHeaderTitle(getTranslation("anon-join-dialog.header", meetupTitle));
    setCloseOnOutsideClick(false);
    setWidth("480px");

    // ── Fields ────────────────────────────────────────────────────────────
    TextField nameField = new TextField(getTranslation("anon-join-dialog.name.label"));
    nameField.setRequired(true);
    nameField.setPlaceholder(getTranslation("anon-join-dialog.name.placeholder"));
    nameField.setWidthFull();

    TextField contactField = new TextField(getTranslation("anon-join-dialog.contact.label"));
    contactField.setRequired(true);
    contactField.setPlaceholder(getTranslation("anon-join-dialog.contact.placeholder"));
    contactField.setHelperText(getTranslation("anon-join-dialog.contact.helper"));
    contactField.setWidthFull();

    // ── GDPR notice ───────────────────────────────────────────────────────
    Paragraph gdprNote = new Paragraph(getTranslation("anon-join-dialog.gdpr.text"));
    gdprNote
        .getStyle()
        .set("font-size", "0.82em")
        .set("color", "var(--lumo-secondary-text-color)")
        .set("line-height", "1.5");

    Checkbox consentBox = new Checkbox(getTranslation("anon-join-dialog.consent.label"));

    // ── Buttons ───────────────────────────────────────────────────────────
    Button cancelBtn = new Button(getTranslation("anon-join-dialog.btn.cancel"), e -> close());
    cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    Button submitBtn = new Button(getTranslation("anon-join-dialog.btn.submit"));
    submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
    submitBtn.addClickListener(
        e -> {
          boolean valid = true;

          if (nameField.getValue() == null || nameField.getValue().isBlank()) {
            nameField.setInvalid(true);
            nameField.setErrorMessage(getTranslation("anon-join-dialog.validation.name-required"));
            valid = false;
          } else {
            nameField.setInvalid(false);
          }

          if (contactField.getValue() == null || contactField.getValue().isBlank()) {
            contactField.setInvalid(true);
            contactField.setErrorMessage(
                getTranslation("anon-join-dialog.validation.contact-required"));
            valid = false;
          } else {
            contactField.setInvalid(false);
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

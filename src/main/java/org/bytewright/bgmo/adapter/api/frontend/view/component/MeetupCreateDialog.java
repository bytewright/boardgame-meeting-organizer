package org.bytewright.bgmo.adapter.api.frontend.view.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import java.time.ZoneId;
import org.bytewright.bgmo.domain.model.MeetupCreation;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.usecases.MeetupWorkflows;

public class MeetupCreateDialog extends Dialog {

  public MeetupCreateDialog(
      RegisteredUser creator, MeetupWorkflows meetupWorkflows, Runnable onSuccess) {

    setHeaderTitle("Create New Meetup");
    setWidth("480px");
    setCloseOnOutsideClick(false);

    // ── Form fields ──────────────────────────────────────────────────────────
    TextField titleField = new TextField("Title");
    titleField.setRequired(true);
    titleField.setWidthFull();
    titleField.setPlaceholder("e.g. Saturday Catan Night");

    TextArea descriptionField = new TextArea("Description");
    descriptionField.setWidthFull();
    descriptionField.setMinHeight("80px");
    descriptionField.setPlaceholder("Optional details about the meetup…");

    DateTimePicker eventDatePicker = new DateTimePicker("Date & Time");
    eventDatePicker.setRequiredIndicatorVisible(true);
    eventDatePicker.setWidthFull();

    IntegerField durationField = new IntegerField("Duration (hours)");
    durationField.setMin(1);
    durationField.setMax(24);
    durationField.setValue(3);
    durationField.setStepButtonsVisible(true);
    durationField.setWidthFull();

    Checkbox unlimitedSlotsCheck = new Checkbox("Unlimited slots");
    unlimitedSlotsCheck.setValue(false);

    IntegerField slotsField = new IntegerField("Max Attendees");
    slotsField.setMin(1);
    slotsField.setValue(8);
    slotsField.setStepButtonsVisible(true);
    slotsField.setWidthFull();
    slotsField.setHelperText("Number of spots available (excluding yourself)");

    // Slot count field is hidden when unlimited is checked
    unlimitedSlotsCheck.addValueChangeListener(e -> slotsField.setVisible(!e.getValue()));

    FormLayout form =
        new FormLayout(
            titleField,
            descriptionField,
            eventDatePicker,
            durationField,
            unlimitedSlotsCheck,
            slotsField);
    form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
    add(form);

    // ── Footer buttons ───────────────────────────────────────────────────────
    Button cancelButton = new Button("Cancel", e -> close());
    cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    Button saveButton =
        new Button(
            "Create Meetup",
            e -> {
              if (titleField.isEmpty()) {
                titleField.setInvalid(true);
                titleField.setErrorMessage("Title is required");
                return;
              }
              if (eventDatePicker.isEmpty()) {
                eventDatePicker.setInvalid(true);
                eventDatePicker.setErrorMessage("Date and time are required");
                return;
              }
              if (durationField.isEmpty() || durationField.getValue() < 1) {
                durationField.setInvalid(true);
                durationField.setErrorMessage("Duration must be at least 1 hour");
                return;
              }

              boolean unlimited = unlimitedSlotsCheck.getValue();
              Integer slots = (!unlimited && !slotsField.isEmpty()) ? slotsField.getValue() : null;

              MeetupCreation creation =
                  MeetupCreation.builder()
                      .title(titleField.getValue().strip())
                      .description(descriptionField.getValue())
                      .eventDate(eventDatePicker.getValue().atZone(ZoneId.systemDefault()))
                      .durationHours(durationField.getValue())
                      .creator(creator)
                      .unlimitedSlots(unlimited)
                      .joinSlots(slots)
                      .build();

              meetupWorkflows.create(creation);

              Notification n =
                  Notification.show("Meetup created!", 3000, Notification.Position.TOP_CENTER);
              n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
              close();
              onSuccess.run();
            });
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    HorizontalLayout footer = new HorizontalLayout(cancelButton, saveButton);
    footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    getFooter().add(footer);
  }
}

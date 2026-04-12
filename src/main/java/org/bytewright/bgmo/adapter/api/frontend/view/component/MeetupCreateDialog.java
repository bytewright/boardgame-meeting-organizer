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

    setHeaderTitle(getTranslation("meetup-creation.title"));
    setWidth("480px");
    setCloseOnOutsideClick(false);

    // ── Form fields ──────────────────────────────────────────────────────────
    TextField titleField = new TextField(getTranslation("meetup-creation.field.title"));
    titleField.setRequired(true);
    titleField.setWidthFull();
    titleField.setPlaceholder(getTranslation("meetup-creation.titlePlaceholder"));

    TextArea descriptionField = new TextArea(getTranslation("meetup-creation.field.desc"));
    descriptionField.setWidthFull();
    descriptionField.setMinHeight("80px");
    descriptionField.setPlaceholder(getTranslation("meetup-creation.field.description"));

    DateTimePicker eventDatePicker =
        new DateTimePicker(getTranslation("meetup-creation.field.date"));
    eventDatePicker.setRequiredIndicatorVisible(true);
    eventDatePicker.setWidthFull();

    IntegerField durationField = new IntegerField(getTranslation("meetup-creation.field.duration"));
    durationField.setMin(1);
    durationField.setMax(24);
    durationField.setValue(3);
    durationField.setStepButtonsVisible(true);
    durationField.setWidthFull();

    Checkbox unlimitedSlotsCheck =
        new Checkbox(getTranslation("meetup-creation.field.slotsUnlimited"));
    unlimitedSlotsCheck.setValue(false);

    IntegerField slotsField = new IntegerField(getTranslation("meetup-creation.field.slots"));
    slotsField.setMin(1);
    slotsField.setValue(3);
    slotsField.setStepButtonsVisible(true);
    slotsField.setWidthFull();
    slotsField.setHelperText(getTranslation("meetup-creation.field.slotsHelper"));

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
    Button cancelButton = new Button(getTranslation("meetup-creation.action.cancel"), e -> close());
    cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    Button saveButton =
        new Button(
            getTranslation("meetup-creation.action.create"),
            e -> {
              if (titleField.isEmpty()) {
                titleField.setInvalid(true);
                titleField.setErrorMessage(getTranslation("meetup-creation.field.titleError"));
                return;
              }
              if (eventDatePicker.isEmpty()) {
                eventDatePicker.setInvalid(true);
                eventDatePicker.setErrorMessage(getTranslation("meetup-creation.field.dateError"));
                return;
              }
              if (durationField.isEmpty() || durationField.getValue() < 1) {
                durationField.setInvalid(true);
                durationField.setErrorMessage(
                    getTranslation("meetup-creation.field.durationError"));
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
                  Notification.show(
                      getTranslation("meetup-creation.success"),
                      3000,
                      Notification.Position.TOP_CENTER);
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

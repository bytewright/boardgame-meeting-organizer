package org.bytewright.bgmo.adapter.api.frontend.view.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
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
import java.time.*;
import org.bytewright.bgmo.domain.model.Game;
import org.bytewright.bgmo.domain.model.MeetupCreation;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.GameDao;
import org.bytewright.bgmo.usecases.MeetupWorkflows;

public class MeetupCreateDialog extends Dialog {

  private final Clock clock;
  private final RegisteredUser creator;
  private final MeetupWorkflows meetupWorkflows;
  private final GameDao gameDao;
  private final Runnable onSuccess;

  public MeetupCreateDialog(
      Clock clock,
      RegisteredUser creator,
      MeetupWorkflows meetupWorkflows,
      GameDao gameDao,
      Runnable onSuccess) {
    this.clock = clock;
    this.creator = creator;
    this.meetupWorkflows = meetupWorkflows;
    this.gameDao = gameDao;
    this.onSuccess = onSuccess;

    setHeaderTitle(getTranslation("meetup-creation.title"));
    setWidth("600px");
    setCloseOnOutsideClick(false);
    createDialogContent();
  }

  private void createDialogContent() {
    // ── Form fields ──────────────────────────────────────────────────────────
    TextField titleField = new TextField(getTranslation("meetup-creation.field.title"));
    titleField.setRequired(true);
    titleField.setWidthFull();
    titleField.setPlaceholder(getTranslation("meetup-creation.titlePlaceholder"));

    TextArea descriptionField = new TextArea(getTranslation("meetup-creation.field.desc"));
    descriptionField.setWidthFull();
    descriptionField.setMinHeight("80px");
    descriptionField.setPlaceholder(getTranslation("meetup-creation.field.description"));

    DateTimePicker registrationClosedDatePicker =
        new DateTimePicker(getTranslation("meetup-creation.field.dateRegistrationClosed"));
    registrationClosedDatePicker.setRequiredIndicatorVisible(true);
    registrationClosedDatePicker.setWidthFull();
    registrationClosedDatePicker.setMin(LocalDateTime.now(clock));
    registrationClosedDatePicker.setValue(nextSaturday(clock));
    DateTimePicker eventDatePicker =
        new DateTimePicker(getTranslation("meetup-creation.field.date"));
    eventDatePicker.setRequiredIndicatorVisible(true);
    eventDatePicker.setWidthFull();
    eventDatePicker.setMin(LocalDateTime.now(clock));
    eventDatePicker.setValue(nextSaturday(clock));
    eventDatePicker.addValueChangeListener(
        event -> {
          registrationClosedDatePicker.setMax(event.getValue());
          registrationClosedDatePicker.setValue(event.getValue().minusHours(1));
        });

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
    // ── Game Selection ──────────────────────────────────────────────────────
    MultiSelectComboBox<Game> gamePicker =
        new MultiSelectComboBox<>(getTranslation("meetup-creation.field.games"));
    gamePicker.setWidthFull();
    gamePicker.setItems(gameDao.findByOwnerId(creator.getId())); // Fetches user's library
    gamePicker.setItemLabelGenerator(Game::getName);
    gamePicker.setPlaceholder(getTranslation("meetup-creation.field.gamesPlaceholder"));

    FormLayout form =
        new FormLayout(
            titleField,
            descriptionField,
            eventDatePicker,
            registrationClosedDatePicker,
            durationField,
            unlimitedSlotsCheck,
            slotsField,
            gamePicker);
    form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
    add(form);

    // ── Footer buttons ───────────────────────────────────────────────────────
    Button cancelButton = new Button(getTranslation("meetup-creation.action.cancel"), e -> close());
    cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    Button saveButton =
        new Button(
            getTranslation("meetup-creation.action.create"),
            e ->
                createMeetup(
                    titleField,
                    eventDatePicker,
                    registrationClosedDatePicker,
                    durationField,
                    unlimitedSlotsCheck,
                    slotsField,
                    descriptionField,
                    gamePicker));
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    HorizontalLayout footer = new HorizontalLayout(cancelButton, saveButton);
    footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    getFooter().add(footer);
  }

  private void createMeetup(
      TextField titleField,
      DateTimePicker eventDatePicker,
      DateTimePicker registrationClosedDatePicker,
      IntegerField durationField,
      Checkbox unlimitedSlotsCheck,
      IntegerField slotsField,
      TextArea descriptionField,
      MultiSelectComboBox<Game> gamePicker) {
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
      durationField.setErrorMessage(getTranslation("meetup-creation.field.durationError"));
      return;
    }

    boolean unlimited = unlimitedSlotsCheck.getValue();
    Integer slots = (!unlimited && !slotsField.isEmpty()) ? slotsField.getValue() : null;

    MeetupCreation creation =
        MeetupCreation.builder()
            .title(titleField.getValue().strip())
            .description(descriptionField.getValue())
            .eventDate(eventDatePicker.getValue().atZone(ZoneId.systemDefault()))
            .registrationClosingDate(
                registrationClosedDatePicker.getValue().atZone(ZoneId.systemDefault()))
            .durationHours(durationField.getValue())
            .creator(creator)
            .unlimitedSlots(unlimited)
            .joinSlots(slots)
            .offeredGames(gamePicker.getValue().stream().map(Game::getId).toList())
            .build();

    meetupWorkflows.create(creation);

    Notification n =
        Notification.show(
            getTranslation("meetup-creation.success"), 3000, Notification.Position.TOP_CENTER);
    n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    close();
    onSuccess.run();
  }

  private LocalDateTime nextSaturday(Clock clock) {
    LocalDate today = LocalDate.now(clock);
    DayOfWeek todayDow = today.getDayOfWeek();

    int daysUntilSaturday = DayOfWeek.SATURDAY.getValue() - todayDow.getValue();
    if (daysUntilSaturday <= 0) {
      daysUntilSaturday += 7;
    }

    LocalDate nextSaturday = today.plusDays(daysUntilSaturday);
    return nextSaturday.atTime(LocalTime.of(18, 0));
  }
}

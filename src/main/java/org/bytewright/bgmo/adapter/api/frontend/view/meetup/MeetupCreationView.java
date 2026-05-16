package org.bytewright.bgmo.adapter.api.frontend.view.meetup;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import jakarta.annotation.security.PermitAll;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.api.frontend.service.SessionInfoService;
import org.bytewright.bgmo.adapter.api.frontend.view.DashboardView;
import org.bytewright.bgmo.adapter.api.frontend.view.LoginView;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.adapter.api.frontend.view.profile.ContactSettingsView;
import org.bytewright.bgmo.domain.model.*;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.model.user.UserStatus;
import org.bytewright.bgmo.domain.service.MeetupLocationService;
import org.bytewright.bgmo.domain.service.MeetupStrategyService;
import org.bytewright.bgmo.domain.service.MeetupStrategyService.SlotDistributionStrategyWithLocalization;
import org.bytewright.bgmo.domain.service.MeetupVisibilityService;
import org.bytewright.bgmo.domain.service.MeetupVisibilityService.MeetupVisibilityStrategyWithLocalization;
import org.bytewright.bgmo.domain.service.data.GameDao;
import org.bytewright.bgmo.usecases.MeetupWorkflows;

/**
 * Full-page meetup creation form, replacing the former {@code MeetupCreateDialog}.
 *
 * <p>Responsibilities:
 *
 * <ol>
 *   <li>Auth guard — redirects unauthenticated visitors to the login page.
 *   <li>Form assembly and input validation.
 *   <li>Building a {@link MeetupEvent.MeetupCreation} DTO and handing it to {@link
 *       MeetupWorkflows#create}.
 *   <li>Navigating to the new meetup's detail page on success.
 * </ol>
 *
 * <p>The registration-closing field is a date picker only. The platform-wide closing time of 20:00
 * (server timezone) is applied by the workflow layer and shown here as a read-only hint.
 *
 * <p>Note: {@link MeetupWorkflows#create} must return the UUID of the newly created meetup so this
 * view can navigate to its detail page.
 */
@Slf4j
@Route(value = "meetup/create", layout = MainLayout.class)
@PageTitle("Create Meetup | " + APP_NAME_SHORT)
@PermitAll
public class MeetupCreationView extends VerticalLayout implements BeforeEnterObserver {

  /** Platform-wide time at which registration closes (server timezone). */
  private static final LocalTime REGISTRATION_CLOSING_TIME = LocalTime.of(20, 0);

  private final Clock clock;
  private final SessionInfoService authService;
  private final MeetupWorkflows meetupWorkflows;
  private final GameDao gameDao;
  private final MeetupStrategyService meetupStrategyService;
  private final MeetupLocationService meetupLocationService;
  private final MeetupVisibilityService meetupVisibilityService;

  private RegisteredUser currentUser;

  public MeetupCreationView(
      Clock clock,
      SessionInfoService authService,
      MeetupWorkflows meetupWorkflows,
      GameDao gameDao,
      MeetupStrategyService meetupStrategyService,
      MeetupLocationService meetupLocationService,
      MeetupVisibilityService meetupVisibilityService) {
    this.clock = clock;
    this.authService = authService;
    this.meetupWorkflows = meetupWorkflows;
    this.gameDao = gameDao;
    this.meetupStrategyService = meetupStrategyService;
    this.meetupLocationService = meetupLocationService;
    this.meetupVisibilityService = meetupVisibilityService;

    setMaxWidth(MainLayout.MAX_DISPLAYPORT_WIDTH);
    getStyle().set("margin", "0 auto");
    setPadding(true);
    setSpacing(true);
  }

  // ── UI construction ───────────────────────────────────────────────────────

  private void buildUI() {
    removeAll();

    // ═══════════════════════════════════════════════════════════════════════
    // Section 1 — Basic information
    // ═══════════════════════════════════════════════════════════════════════
    add(sectionHeader(getTranslation("meetup-creation.section.basics")));

    TextField titleField = new TextField(getTranslation("meetup-creation.field.title"));
    titleField.setRequired(true);
    titleField.setWidthFull();
    titleField.setPlaceholder(getTranslation("meetup-creation.titlePlaceholder"));

    TextArea descriptionField = new TextArea(getTranslation("meetup-creation.field.desc"));
    descriptionField.setWidthFull();
    descriptionField.setMinHeight("80px");
    descriptionField.setPlaceholder(getTranslation("meetup-creation.field.descPlaceholder"));

    MultiSelectComboBox<Game> gamePicker =
        new MultiSelectComboBox<>(getTranslation("meetup-creation.field.games"));
    gamePicker.setWidthFull();
    gamePicker.setItems(gameDao.findByOwnerId(currentUser.getId()));
    gamePicker.setItemLabelGenerator(Game::getName);
    gamePicker.setPlaceholder(getTranslation("meetup-creation.field.gamesPlaceholder"));

    FormLayout basicsForm = uniformForm(titleField, descriptionField, gamePicker);
    add(basicsForm);

    // ═══════════════════════════════════════════════════════════════════════
    // Section 2 — Schedule
    // ═══════════════════════════════════════════════════════════════════════
    add(new Hr(), sectionHeader(getTranslation("meetup-creation.section.schedule")));

    LocalDateTime defaultEventDate = nextSaturday(clock);

    DateTimePicker eventDatePicker =
        new DateTimePicker(getTranslation("meetup-creation.field.date"));
    eventDatePicker.setRequiredIndicatorVisible(true);
    eventDatePicker.setWidthFull();
    eventDatePicker.setMin(LocalDateTime.now(clock));
    eventDatePicker.setValue(defaultEventDate);

    // Registration-closing: date only — time is fixed at REGISTRATION_CLOSING_TIME
    DatePicker registrationDeadlinePicker =
        new DatePicker(getTranslation("meetup-creation.field.dateRegistrationClosed"));
    registrationDeadlinePicker.setRequiredIndicatorVisible(true);
    registrationDeadlinePicker.setWidthFull();
    registrationDeadlinePicker.setMin(LocalDate.now(clock));
    registrationDeadlinePicker.setMax(defaultEventDate.toLocalDate());
    registrationDeadlinePicker.setValue(defaultEventDate.toLocalDate().minusDays(1));
    registrationDeadlinePicker.setHelperText(
        getTranslation(
            "meetup-creation.field.registrationTimeHint", REGISTRATION_CLOSING_TIME.toString()));

    // Keep deadline bounded by event date
    eventDatePicker.addValueChangeListener(
        e -> {
          if (e.getValue() != null) {
            LocalDate eventDate = e.getValue().toLocalDate();
            registrationDeadlinePicker.setMax(eventDate);
            // Nudge deadline one day earlier whenever the event date moves
            if (registrationDeadlinePicker.getValue() == null
                || !registrationDeadlinePicker.getValue().isBefore(eventDate)) {
              registrationDeadlinePicker.setValue(eventDate.minusDays(1));
            }
          }
        });

    IntegerField durationField = new IntegerField(getTranslation("meetup-creation.field.duration"));
    durationField.setMin(1);
    durationField.setMax(24);
    durationField.setValue(3);
    durationField.setStepButtonsVisible(true);
    durationField.setWidthFull();
    durationField.setHelperText(getTranslation("meetup-creation.field.durationHelper"));

    add(uniformForm(eventDatePicker, registrationDeadlinePicker, durationField));

    // ═══════════════════════════════════════════════════════════════════════
    // Section 3 — Attendees & slot distribution
    // ═══════════════════════════════════════════════════════════════════════
    add(new Hr(), sectionHeader(getTranslation("meetup-creation.section.attendees")));

    Checkbox unlimitedSlotsCheck =
        new Checkbox(getTranslation("meetup-creation.field.slotsUnlimited"));
    unlimitedSlotsCheck.setValue(false);

    IntegerField slotsField = new IntegerField(getTranslation("meetup-creation.field.slots"));
    slotsField.setMin(1);
    slotsField.setValue(3);
    slotsField.setStepButtonsVisible(true);
    slotsField.setWidthFull();
    slotsField.setHelperText(getTranslation("meetup-creation.field.slotsHelper"));

    unlimitedSlotsCheck.addValueChangeListener(e -> slotsField.setEnabled(!e.getValue()));

    // Slot distribution strategy
    var slotDistributionStrategies =
        meetupStrategyService.getAvailableSlotDistributionStrategies(UI.getCurrent().getLocale());

    Select<SlotDistributionStrategyWithLocalization> strategySelect = new Select<>();
    strategySelect.setLabel(getTranslation("meetup-creation.field.slotStrategy"));
    strategySelect.setWidthFull();
    strategySelect.setItems(slotDistributionStrategies);
    strategySelect.setItemLabelGenerator(SlotDistributionStrategyWithLocalization::getDisplayName);
    // Mirror each strategy's help text into the field helper on selection
    strategySelect.addValueChangeListener(
        e -> {
          if (e.getValue() != null) {
            strategySelect.setHelperText(e.getValue().getHelpText());
          }
        });
    strategySelect.setValue(slotDistributionStrategies.getFirst());

    var visibilityStrategies =
        meetupVisibilityService.getAvailableMeetupVisibilityStrategies(UI.getCurrent().getLocale());
    Select<MeetupVisibilityStrategyWithLocalization> visibilitySelect = new Select<>();
    visibilitySelect.setLabel(getTranslation("meetup-creation.field.visibilityStrategy"));
    visibilitySelect.setWidthFull();
    visibilitySelect.setItems(visibilityStrategies);
    visibilitySelect.setItemLabelGenerator(
        MeetupVisibilityStrategyWithLocalization::getDisplayName);
    // Mirror each strategy's help text into the field helper on selection
    visibilitySelect.addValueChangeListener(
        e -> {
          if (e.getValue() != null) {
            visibilitySelect.setHelperText(e.getValue().getHelpText());
          }
        });
    visibilitySelect.setValue(visibilityStrategies.getFirst());

    Checkbox onlyMembersToggle = new Checkbox(getTranslation("meetup-creation.field.allowAnon"));
    onlyMembersToggle.setValue(false);

    FormLayout attendeesForm =
        uniformForm(
            unlimitedSlotsCheck, slotsField, strategySelect, visibilitySelect, onlyMembersToggle);
    add(attendeesForm);

    // ═══════════════════════════════════════════════════════════════════════
    // Section 4 — Location
    // ═══════════════════════════════════════════════════════════════════════
    add(new Hr(), sectionHeader(getTranslation("meetup-creation.section.location")));

    // Suggestion picker — pre-fills the two fields below
    List<MeetupLocationSuggestion> suggestions =
        meetupLocationService.getSuggestedLocations(currentUser.getId());

    ComboBox<MeetupLocationSuggestion> suggestionPicker = new ComboBox<>();
    suggestionPicker.setLabel(getTranslation("meetup-creation.field.locationSaved"));
    suggestionPicker.setWidthFull();
    suggestionPicker.setItems(suggestions);
    // Common (admin-curated) locations are prefixed with a star so they stand out
    suggestionPicker.setItemLabelGenerator(
        s -> (s.isCommon() ? "★  " : "") + s.location().areaHint());
    suggestionPicker.setPlaceholder(
        getTranslation("meetup-creation.field.locationSavedPlaceholder"));
    suggestionPicker.setClearButtonVisible(true);
    suggestionPicker.setHelperText(getTranslation("meetup-creation.field.locationSavedHelper"));

    TextField areaHintField =
        new TextField(getTranslation("meetup-creation.field.locationAreaHint"));
    areaHintField.setRequired(true);
    areaHintField.setWidthFull();
    areaHintField.setPlaceholder(
        getTranslation("meetup-creation.field.locationAreaHintPlaceholder"));
    areaHintField.setHelperText(getTranslation("meetup-creation.field.locationAreaHintHelper"));

    TextArea fullLocationField = new TextArea(getTranslation("meetup-creation.field.locationFull"));
    fullLocationField.setRequired(true);
    fullLocationField.setWidthFull();
    fullLocationField.setMinHeight("80px");
    fullLocationField.setPlaceholder(
        getTranslation("meetup-creation.field.locationFullPlaceholder"));
    fullLocationField.setHelperText(getTranslation("meetup-creation.field.locationFullHelper"));

    // Populate fields when a suggestion is selected
    suggestionPicker.addValueChangeListener(
        e -> {
          if (e.getValue() != null) {
            areaHintField.setValue(e.getValue().location().areaHint());
            areaHintField.setInvalid(false);
            fullLocationField.setValue(e.getValue().location().fullLocation());
            fullLocationField.setInvalid(false);
          }
        });

    add(uniformForm(suggestionPicker, areaHintField, fullLocationField));

    // ═══════════════════════════════════════════════════════════════════════
    // Footer actions
    // ═══════════════════════════════════════════════════════════════════════
    add(new Hr());

    Button cancelButton =
        new Button(
            getTranslation("meetup-creation.action.cancel"),
            e -> UI.getCurrent().navigate(DashboardView.class));
    cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    Button createButton =
        new Button(
            getTranslation("meetup-creation.action.create"),
            e ->
                handleCreate(
                    titleField,
                    descriptionField,
                    gamePicker,
                    eventDatePicker,
                    registrationDeadlinePicker,
                    durationField,
                    unlimitedSlotsCheck,
                    slotsField,
                    strategySelect,
                    visibilitySelect,
                    onlyMembersToggle,
                    areaHintField,
                    fullLocationField));
    createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    HorizontalLayout footer = new HorizontalLayout(cancelButton, createButton);
    footer.setWidthFull();
    footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    add(footer);
  }

  // ── Submission ────────────────────────────────────────────────────────────

  private void handleCreate(
      TextField titleField,
      TextArea descriptionField,
      MultiSelectComboBox<Game> gamePicker,
      DateTimePicker eventDatePicker,
      DatePicker registrationDeadlinePicker,
      IntegerField durationField,
      Checkbox unlimitedSlotsCheck,
      IntegerField slotsField,
      Select<SlotDistributionStrategyWithLocalization> strategySelect,
      Select<MeetupVisibilityStrategyWithLocalization> visibilitySelect,
      Checkbox onlyMembersToggle,
      TextField areaHintField,
      TextArea fullLocationField) {

    if (!validate(
        titleField,
        eventDatePicker,
        registrationDeadlinePicker,
        durationField,
        areaHintField,
        fullLocationField)) {
      return;
    }

    boolean unlimited = unlimitedSlotsCheck.getValue();
    Integer slots = (!unlimited && !slotsField.isEmpty()) ? slotsField.getValue() : null;

    MeetupEvent.MeetupCreation creation =
        MeetupEvent.MeetupCreation.builder()
            .title(titleField.getValue().strip())
            .description(descriptionField.getValue())
            .eventDate(eventDatePicker.getValue().atZone(ZoneId.systemDefault()))
            .registrationClosingDate(registrationDeadlinePicker.getValue())
            .durationHours(durationField.getValue())
            .creator(currentUser)
            .unlimitedSlots(unlimited)
            .joinSlots(slots)
            .slotStrategy(strategySelect.getValue().getStrategy())
            .allowAnonSignup(!onlyMembersToggle.getValue())
            .visibility(visibilitySelect.getValue().getStrategy())
            .offeredGames(gamePicker.getValue().stream().map(Game::getId).toList())
            .location(
                new MeetupEventLocation(
                    areaHintField.getValue().strip(), fullLocationField.getValue().strip()))
            .build();

    // MeetupWorkflows#create must return the UUID of the new meetup
    MeetupEvent newMeetup = meetupWorkflows.create(creation);

    Notification n =
        Notification.show(
            getTranslation("meetup-creation.success"), 3000, Notification.Position.TOP_CENTER);
    n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

    UI.getCurrent()
        .navigate(MeetupDetailView.class, new RouteParam("meetupId", newMeetup.getId().toString()));
  }

  // ── Validation ────────────────────────────────────────────────────────────

  /**
   * Validates all required fields and marks invalid ones in-place.
   *
   * @return {@code true} if the form is ready to submit
   */
  private boolean validate(
      TextField titleField,
      DateTimePicker eventDatePicker,
      DatePicker registrationDeadlinePicker,
      IntegerField durationField,
      TextField areaHintField,
      TextArea fullLocationField) {

    boolean valid = true;

    if (titleField.isEmpty()) {
      titleField.setInvalid(true);
      titleField.setErrorMessage(getTranslation("meetup-creation.field.titleError"));
      valid = false;
    }
    if (eventDatePicker.isEmpty()) {
      eventDatePicker.setInvalid(true);
      eventDatePicker.setErrorMessage(getTranslation("meetup-creation.field.dateError"));
      valid = false;
    }
    if (registrationDeadlinePicker.isEmpty()) {
      registrationDeadlinePicker.setInvalid(true);
      registrationDeadlinePicker.setErrorMessage(
          getTranslation("meetup-creation.field.dateRegistrationClosedError"));
      valid = false;
    }
    if (durationField.isEmpty() || durationField.getValue() < 1) {
      durationField.setInvalid(true);
      durationField.setErrorMessage(getTranslation("meetup-creation.field.durationError"));
      valid = false;
    }
    if (areaHintField.isEmpty()) {
      areaHintField.setInvalid(true);
      areaHintField.setErrorMessage(getTranslation("meetup-creation.field.locationAreaHintError"));
      valid = false;
    }
    if (fullLocationField.isEmpty()) {
      fullLocationField.setInvalid(true);
      fullLocationField.setErrorMessage(getTranslation("meetup-creation.field.locationFullError"));
      valid = false;
    }

    return valid;
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  /** Builds a single-column {@link FormLayout} — matches the responsive pattern used app-wide. */
  private static FormLayout uniformForm(com.vaadin.flow.component.Component... fields) {
    FormLayout form = new FormLayout(fields);
    form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
    form.setWidthFull();
    return form;
  }

  /** Small styled section header, visually separating groups of fields. */
  private Span sectionHeader(String text) {
    Span header = new Span(text);
    header
        .getStyle()
        .set("font-size", "var(--lumo-font-size-s)")
        .set("font-weight", "600")
        .set("color", "var(--lumo-secondary-text-color)")
        .set("text-transform", "uppercase")
        .set("letter-spacing", "0.08em");
    return header;
  }

  /** Returns 18:00 on the next Saturday relative to today. */
  private static LocalDateTime nextSaturday(Clock clock) {
    LocalDate today = LocalDate.now(clock);
    int daysUntilSaturday = DayOfWeek.SATURDAY.getValue() - today.getDayOfWeek().getValue();
    if (daysUntilSaturday <= 0) {
      daysUntilSaturday += 7;
    }
    return today.plusDays(daysUntilSaturday).atTime(LocalTime.of(18, 0));
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    authService
        .getCurrentUser()
        .ifPresentOrElse(
            user -> {
              if (user.getStatus() != UserStatus.ACTIVE || user.getContactOptions().isEmpty()) {
                event.forwardTo(ContactSettingsView.class);
              } else {
                this.currentUser = user;
                buildUI();
              }
            },
            () -> {
              log.debug(
                  "Unauthenticated visitor tried to access MeetupCreationView — forwarding to login");
              event.forwardTo(LoginView.class);
            });
  }
}

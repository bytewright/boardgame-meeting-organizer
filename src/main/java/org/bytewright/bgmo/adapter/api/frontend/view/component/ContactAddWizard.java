package org.bytewright.bgmo.adapter.api.frontend.view.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Data;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.service.user.ContactInfoValidationService;

public class ContactAddWizard extends Dialog {

  private final Consumer<ContactInfo> onComplete;
  private final ContactInfoValidationService validationService;

  private final FormLayout dynamicFieldsLayout;
  private final Button saveBtn;
  private final ComboBox<ContactInfoType> typePicker;

  private Binder<ContactDraft> binder;
  private ContactDraft currentDraft;
  private ContactInfoType currentType;
  private UUID existingId; // for Edit mode

  // Constructor for ADD mode
  public ContactAddWizard(
      Consumer<ContactInfo> onComplete, ContactInfoValidationService validationService) {
    this(null, onComplete, validationService);
  }

  // Constructor for EDIT mode (and shared logic)
  public ContactAddWizard(
      ContactInfo existingContact,
      Consumer<ContactInfo> onComplete,
      ContactInfoValidationService validationService) {
    this.onComplete = onComplete;
    this.validationService = validationService;

    boolean isEditMode = existingContact != null;
    setHeaderTitle(isEditMode ? "Edit Contact Information" : "Add Contact Information");

    typePicker = new ComboBox<>("Contact Type", ContactInfoType.values());
    typePicker.setWidthFull();

    dynamicFieldsLayout = new FormLayout();
    dynamicFieldsLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

    // Pass the existing contact down when the type changes (or is initialized)
    typePicker.addValueChangeListener(e -> buildFormForType(e.getValue(), existingContact));

    saveBtn = new Button("Save", e -> saveContact());
    saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveBtn.setEnabled(false);

    Button cancelBtn = new Button("Cancel", e -> close());

    VerticalLayout layout = new VerticalLayout(typePicker, dynamicFieldsLayout);
    layout.setPadding(false);
    add(layout);

    getFooter().add(cancelBtn, saveBtn);

    if (isEditMode) {
      this.existingId = existingContact.id();
      typePicker.setValue(existingContact.type());
      typePicker.setReadOnly(true); // Prevent changing type while editing
    }
  }

  private void buildFormForType(ContactInfoType type, ContactInfo existingContact) {
    dynamicFieldsLayout.removeAll();
    this.currentType = type;

    if (type == null) {
      saveBtn.setEnabled(false);
      return;
    }

    this.currentDraft = new ContactDraft();
    this.binder = new Binder<>(ContactDraft.class);
    this.binder.addStatusChangeListener(e -> saveBtn.setEnabled(binder.isValid()));

    // Pre-fill draft if in edit mode
    if (existingContact != null && existingContact.type() == type) {
      switch (existingContact) {
        case ContactInfo.EmailContact e -> currentDraft.setSingleValue(e.email());
        case ContactInfo.TelegramContact t -> currentDraft.setSingleValue(t.chatId());
        case ContactInfo.SignalContact s -> currentDraft.setSingleValue(s.signalHandle());
        case ContactInfo.PhoneContact p -> currentDraft.setSingleValue(p.phoneNr());
        case ContactInfo.AddressContact a -> {
          currentDraft.setStreet(a.street());
          currentDraft.setZipCode(a.zipCode());
          currentDraft.setCity(a.city());
        }
      }
    }

    switch (type) {
      case EMAIL ->
          buildSingleFieldForm(
              "Email Address",
              VaadinIcon.ENVELOPE,
              validationService::validateEmail,
              "Invalid email format");
      case TELEGRAM ->
          buildSingleFieldForm(
              "Telegram Chat ID / Handle",
              VaadinIcon.PAPERPLANE,
              validationService::validateTelegram,
              "Invalid Telegram handle");
      case SIGNAL ->
          buildSingleFieldForm(
              "Signal Handle",
              VaadinIcon.CHAT,
              validationService::validateSignal,
              "Invalid Signal handle");
      case PHONE ->
          buildSingleFieldForm(
              "Phone Number",
              VaadinIcon.PHONE,
              validationService::validatePhone,
              "Invalid phone format");
      case ADDRESS -> buildAddressForm();
    }

    binder.setBean(currentDraft);

    // If we pre-filled valid data, the save button should be active
    saveBtn.setEnabled(binder.isValid());
  }

  private void buildSingleFieldForm(
      String label, VaadinIcon icon, ValidatorFunc validator, String errorMsg) {
    TextField valueField = new TextField(label);
    valueField.setWidthFull();
    valueField.setPrefixComponent(icon.create());

    binder
        .forField(valueField)
        .asRequired("This field is required")
        .withValidator(validator::validate, errorMsg)
        .bind(ContactDraft::getSingleValue, ContactDraft::setSingleValue);

    dynamicFieldsLayout.add(valueField);
  }

  private void buildAddressForm() {
    TextField streetField = new TextField("Street & Number");
    streetField.setPrefixComponent(VaadinIcon.HOME.create());

    TextField zipField = new TextField("ZIP Code");
    TextField cityField = new TextField("City");

    binder
        .forField(streetField)
        .asRequired("Street is required")
        .bind(ContactDraft::getStreet, ContactDraft::setStreet);

    binder
        .forField(zipField)
        .asRequired("ZIP is required")
        // Example of inline validation if the service handles the whole address at once:
        .bind(ContactDraft::getZipCode, ContactDraft::setZipCode);

    binder
        .forField(cityField)
        .asRequired("City is required")
        .bind(ContactDraft::getCity, ContactDraft::setCity);

    // If your service validates the whole address combination:
    binder.withValidator(
        draft ->
            validationService.validateAddress(
                draft.getStreet(), draft.getZipCode(), draft.getCity()),
        "Address does not appear to be valid");

    dynamicFieldsLayout.add(streetField, zipField, cityField);
  }

  private void saveContact() {
    if (binder.validate().isOk()) {
      ContactInfo newContact =
          switch (currentType) {
            case EMAIL ->
                ContactInfo.EmailContact.builder()
                    .id(existingId)
                    .email(currentDraft.getSingleValue())
                    .build();
            case TELEGRAM ->
                ContactInfo.TelegramContact.builder()
                    .id(existingId)
                    .chatId(currentDraft.getSingleValue())
                    .build();
            case SIGNAL ->
                ContactInfo.SignalContact.builder()
                    .id(existingId)
                    .signalHandle(currentDraft.getSingleValue())
                    .build();
            case PHONE ->
                ContactInfo.PhoneContact.builder()
                    .id(existingId)
                    .phoneNr(currentDraft.getSingleValue())
                    .build();
            case ADDRESS ->
                ContactInfo.AddressContact.builder()
                    .id(existingId)
                    .street(currentDraft.getStreet())
                    .zipCode(currentDraft.getZipCode())
                    .city(currentDraft.getCity())
                    .build();
          };
      onComplete.accept(newContact);
      close();
    }
  }

  @FunctionalInterface
  private interface ValidatorFunc {
    boolean validate(String value);
  }

  @Data
  private static class ContactDraft {
    private String singleValue; // Used for Email, Phone, Telegram, Signal
    private String street;
    private String zipCode;
    private String city;
  }
}

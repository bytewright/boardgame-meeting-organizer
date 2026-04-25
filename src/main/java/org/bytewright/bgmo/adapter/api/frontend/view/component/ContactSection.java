package org.bytewright.bgmo.adapter.api.frontend.view.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactInfo.*;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.user.ContactInfoValidationService;
import org.bytewright.bgmo.usecases.UserWorkflows;

@Slf4j
public class ContactSection extends VerticalLayout {

  private final RadioButtonGroup<ContactInfo> favoriteSelector;
  private final ContactInfoValidationService validationService;
  private final UserWorkflows userWorkflows;
  private RegisteredUser currentUser;

  public ContactSection(
      ContactInfoValidationService validationService,
      UserWorkflows userWorkflows,
      RegisteredUser currentUser) {
    this.validationService = validationService;
    this.userWorkflows = userWorkflows;
    this.currentUser = currentUser;

    VerticalLayout listContainer = new VerticalLayout();
    listContainer.setPadding(false);
    listContainer.setSpacing(true);

    this.favoriteSelector = new RadioButtonGroup<>();
    this.favoriteSelector.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
    this.favoriteSelector.setLabel(getTranslation("profile.contacts.favorite.label"));
    this.favoriteSelector.setRenderer(new ComponentRenderer<>(this::renderContactRow));

    this.favoriteSelector.addValueChangeListener(
        event -> {
          if (event.getValue() != null) {
            updatePrimaryContact(event.getValue());
          }
        });

    Button addContactBtn =
        new Button(getTranslation("profile.contacts.add"), VaadinIcon.PLUS.create());
    addContactBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    addContactBtn.addClickListener(e -> openAddContactWizard());

    add(favoriteSelector, addContactBtn);
    refreshContacts();
  }

  private void refreshContacts() {
    currentUser = userWorkflows.refreshUser(currentUser);
    List<ContactInfo> contacts = new ArrayList<>(currentUser.getContactInfos());
    favoriteSelector.setItems(contacts);

    ContactInfo primary =
        contacts.stream()
            .filter(c -> c.id().equals(currentUser.getPrimaryContactId()))
            .findFirst()
            .orElse(null);
    favoriteSelector.setValue(primary);
  }

  private Component renderContactRow(ContactInfo info) {
    HorizontalLayout row = new HorizontalLayout();
    row.setWidthFull();
    row.setAlignItems(FlexComponent.Alignment.CENTER);
    row.getStyle().set("padding", "var(--lumo-space-s)");
    row.getStyle().set("background", "var(--lumo-contrast-5pct)");
    row.getStyle().set("border-radius", "var(--lumo-border-radius-m)");

    Component icon = getIconForType(info);
    VerticalLayout details = new VerticalLayout(new Span(info.type().name()));
    details.setPadding(false);
    details.setSpacing(false);

    Span content =
        switch (info) {
          case EmailContact email -> new Span(email.email());
          case PhoneContact phone -> new Span(phone.phoneNr());
          case TelegramContact tg -> new Span("@" + tg.chatId());
          case SignalContact sig -> new Span(sig.signalHandle());
          case AddressContact addr ->
              new Span("%s, %s %s".formatted(addr.street(), addr.zipCode(), addr.city()));
        };
    content.getStyle().set("font-size", "var(--lumo-font-size-s)");
    content.getStyle().set("color", "var(--lumo-secondary-text-color)");
    details.add(content);

    Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> deleteContact(info));
    deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
    // TODO add edit functionality - this should best reuse the ContactAddWizard
    row.add(icon, details, deleteBtn);
    row.expand(details);
    return row;
  }

  private Component getIconForType(ContactInfo info) {
    return switch (info.type()) {
      case EMAIL -> VaadinIcon.ENVELOPE.create();
      case PHONE -> VaadinIcon.PHONE.create();
      case ADDRESS -> VaadinIcon.HOME.create();
      case TELEGRAM, SIGNAL -> VaadinIcon.CHAT.create();
    };
  }

  private void openAddContactWizard() {
    ContactAddWizard dialog =
        new ContactAddWizard(
            contactInfo -> {
              userWorkflows.addContactInfo(currentUser.getId(), contactInfo);
              refreshContacts();
            },
            validationService);
    dialog.open();
    dialog.addOpenedChangeListener(
        event -> {
          if (!event.isOpened()) {
            refreshContacts();
          }
        });
  }

  private void updatePrimaryContact(ContactInfo contactInfo) {
    userWorkflows.changePrimaryContactInfo(currentUser.getId(), contactInfo);
  }

  private void deleteContact(ContactInfo info) {
    // TODO: Add confirmation dialog to really delete
    userWorkflows.removeContact(currentUser.getId(), info);
    refreshContacts();
  }
}

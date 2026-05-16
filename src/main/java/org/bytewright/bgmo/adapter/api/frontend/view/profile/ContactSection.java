package org.bytewright.bgmo.adapter.api.frontend.view.profile;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.bytewright.bgmo.adapter.api.frontend.view.component.factory.ComponentFactory;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.model.user.ContactOption;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.model.user.exception.ModifyContactsException;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.usecases.UserWorkflows;

/**
 * Contact management section for ProfileView.
 *
 * <p>Messenger types (Telegram, Signal): one per type max, link-only via bot code dialog. Freeform
 * types (Email, Phone, Address): multiple allowed, inline add form. All existing entries: remove
 * only, no editing.
 */
public class ContactSection extends VerticalLayout {

  private final ComponentFactory componentFactory;
  private final UserWorkflows userWorkflows;
  private final RegisteredUserDao userDao;
  private final Runnable runnable;
  private RegisteredUser currentUser;

  public ContactSection(
      ComponentFactory componentFactory,
      UserWorkflows userWorkflows,
      RegisteredUserDao userDao,
      RegisteredUser currentUser,
      Runnable runnable) {
    this.componentFactory = componentFactory;
    this.userWorkflows = userWorkflows;
    this.userDao = userDao;
    this.runnable = runnable;

    setPadding(false);
    setSpacing(false);

    rebuild(currentUser.getId());
  }

  private void rebuild(UUID userId) {
    removeAll();
    currentUser = userDao.findOrThrow(userId);

    List<ContactOption> contacts = List.copyOf(currentUser.getContactOptions());
    add(buildMessengerSection(contacts, ContactInfoType.TELEGRAM));
    add(new Hr());
    add(buildMessengerSection(contacts, ContactInfoType.SIGNAL));
    add(new Hr());
    add(buildFreeformSection(contacts, ContactInfoType.EMAIL));
    add(new Hr());
    add(buildFreeformSection(contacts, ContactInfoType.PHONE));
    add(new Hr());
    add(buildFreeformSection(contacts, ContactInfoType.ADDRESS));
  }

  // -------------------------------------------------------------------------
  // Messenger section (one per type, link-via-bot only)
  // -------------------------------------------------------------------------

  private Component buildMessengerSection(List<ContactOption> contacts, ContactInfoType type) {
    String typeLabel = ContactInfoLabelUtil.messengerName(type);
    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.setSpacing(false);
    section.add(sectionLabel(typeLabel));

    Component component =
        contacts.stream()
            .filter(c -> c.getType() == type)
            .findFirst()
            .map(linked -> buildLinkedMessengerRow(linked, type))
            .orElse(buildLinkButton(type));
    section.add(component);
    return section;
  }

  private Component buildLinkedMessengerRow(ContactOption contact, ContactInfoType type) {
    Span handle = new Span(ContactInfoLabelUtil.displayValue(contact.getContactInfo()));
    handle.getStyle().set("font-weight", "500");

    Span verifiedBadge = new Span(contact.isVerified() ? " ✓ Verified" : " ⚠ Unverified");
    verifiedBadge
        .getStyle()
        .set(
            "color",
            contact.isVerified() ? "var(--lumo-success-color)" : "var(--lumo-warning-color)");
    verifiedBadge.getStyle().set("font-size", "var(--lumo-font-size-s)");

    Button unlinkBtn = new Button("Unlink", VaadinIcon.UNLINK.create());
    unlinkBtn.addThemeVariants(
        ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    unlinkBtn.addClickListener(
        e -> {
          try {
            userWorkflows.removeContact(currentUser.getId(), contact);
            Notification n =
                Notification.show(ContactInfoLabelUtil.messengerName(type) + " unlinked.");
            n.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            runnable.run();
          } catch (ModifyContactsException ex) {
            Notification n = Notification.show(getTranslation(ex.getMessageKey()));
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
          }
        });

    HorizontalLayout row = new HorizontalLayout(handle, verifiedBadge, unlinkBtn);
    row.setAlignItems(Alignment.CENTER);
    row.setJustifyContentMode(JustifyContentMode.BETWEEN);
    row.setWidthFull();
    row.getStyle().set("padding", "var(--lumo-space-s) 0");
    return row;
  }

  private Component buildLinkButton(ContactInfoType type) {
    Button linkBtn =
        new Button(
            "Link " + ContactInfoLabelUtil.messengerName(type) + " account",
            VaadinIcon.LINK.create());
    linkBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    linkBtn.addClickListener(
        e -> {
          MessengerLinkDialog dialog =
              componentFactory.messengerLinkDialog(
                  currentUser.getId(),
                  type,
                  () -> {
                    Notification n =
                        Notification.show(
                            ContactInfoLabelUtil.messengerName(type) + " linked successfully!");
                    n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    runnable.run();
                  });
          dialog.open();
        });
    linkBtn.getStyle().set("margin-top", "var(--lumo-space-xs)");
    return linkBtn;
  }

  // -------------------------------------------------------------------------
  // Freeform section (Email, Phone, Address — multiple allowed)
  // -------------------------------------------------------------------------

  private Component buildFreeformSection(List<ContactOption> contacts, ContactInfoType type) {
    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.setSpacing(false);

    section.add(sectionLabel(ContactInfoLabelUtil.labelFor(type)));

    contacts.stream()
        .filter(c -> c.getType() == type)
        .sorted(Comparator.comparing(ContactOption::getTsCreation))
        .forEach(c -> section.add(buildFreeformRow(c)));

    VerticalLayout addForm = buildAddForm(type);
    addForm.setVisible(false);

    Button addBtn = new Button(getTranslation(ContactInfoLabelUtil.translationKeyForAdd(type)));
    addBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    addBtn.getStyle().set("margin-top", "var(--lumo-space-xs)");
    addBtn.addClickListener(e -> addForm.setVisible(true));

    section.add(addBtn, addForm);
    return section;
  }

  private Component buildFreeformRow(ContactOption contact) {
    Span value = new Span(ContactInfoLabelUtil.displayValue(contact.getContactInfo()));
    value.getStyle().set("font-size", "var(--lumo-font-size-s)").set("word-break", "break-all");

    Button removeBtn = new Button(VaadinIcon.TRASH.create());
    removeBtn.addThemeVariants(
        ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY,
        ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
    removeBtn.getElement().setAttribute("aria-label", "Remove");
    removeBtn.addClickListener(
        e -> {
          try {
            userWorkflows.removeContact(currentUser.getId(), contact);
            rebuild(currentUser.getId());
          } catch (ModifyContactsException ex) {
            Notification n = Notification.show(getTranslation(ex.getMessageKey()));
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
          }
        });

    HorizontalLayout row = new HorizontalLayout(value, removeBtn);
    row.setAlignItems(Alignment.CENTER);
    row.setJustifyContentMode(JustifyContentMode.BETWEEN);
    row.setWidthFull();
    row.getStyle()
        .set("padding", "var(--lumo-space-xs) 0")
        .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
    return row;
  }

  private VerticalLayout buildAddForm(ContactInfoType type) {
    VerticalLayout form = new VerticalLayout();
    form.setPadding(true);
    form.setSpacing(true);
    form.getStyle()
        .set("margin-top", "var(--lumo-space-s)")
        .set("border-radius", "var(--lumo-border-radius-m)")
        .set("background-color", "var(--lumo-contrast-5pct)");

    switch (type) {
      case EMAIL -> {
        TextField emailField = new TextField("Email address");
        emailField.setWidthFull();
        emailField.setPlaceholder("name@example.com");
        emailField.setHelperText(
            getTranslation(ContactInfoLabelUtil.translationKeyForExplain(ContactInfoType.EMAIL)));
        form.add(
            emailField,
            saveRow(
                () -> {
                  userWorkflows.addContactInfo(
                      currentUser.getId(),
                      ContactInfo.EmailContact.builder().email(emailField.getValue()).build(),
                      false);
                  rebuild(currentUser.getId());
                },
                () -> form.setVisible(false)));
      }
      case PHONE -> {
        TextField phoneField = new TextField("Phone number");
        phoneField.setWidthFull();
        phoneField.setPlaceholder("+49 123 456789");
        phoneField.setHelperText(
            getTranslation(ContactInfoLabelUtil.translationKeyForExplain(ContactInfoType.PHONE)));
        form.add(
            phoneField,
            saveRow(
                () -> {
                  userWorkflows.addContactInfo(
                      currentUser.getId(),
                      ContactInfo.PhoneContact.builder().phoneNr(phoneField.getValue()).build(),
                      false);
                  rebuild(currentUser.getId());
                },
                () -> form.setVisible(false)));
      }
      case ADDRESS -> {
        TextField nameOnBell = new TextField("Name on bell");
        TextField street = new TextField("Street & Nr.");
        TextField zip = new TextField("ZIP");
        TextField city = new TextField("City");
        TextField comment = new TextField("Comment (optional)");
        for (var f : List.of(nameOnBell, street, zip, city, comment)) {
          f.setWidthFull();
        }
        form.add(
            new Span(
                getTranslation(
                    ContactInfoLabelUtil.translationKeyForExplain(ContactInfoType.ADDRESS))),
            nameOnBell,
            street,
            zip,
            city,
            comment,
            saveRow(
                () -> {
                  userWorkflows.addContactInfo(
                      currentUser.getId(),
                      ContactInfo.AddressContact.builder()
                          .nameOnBell(nameOnBell.getValue())
                          .street(street.getValue())
                          .zipCode(zip.getValue())
                          .city(city.getValue())
                          .comment(comment.getValue())
                          .build(),
                      false);
                  rebuild(currentUser.getId());
                },
                () -> form.setVisible(false)));
      }
      case TELEGRAM, SIGNAL -> throw new IllegalArgumentException();
    }

    return form;
  }

  private HorizontalLayout saveRow(Runnable onSave, Runnable onCancel) {
    Button saveBtn = new Button("Save", VaadinIcon.CHECK.create());
    saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
    saveBtn.addClickListener(
        e -> {
          onSave.run();
          onCancel.run();
        });

    Button cancelBtn = new Button("Cancel");
    cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    cancelBtn.addClickListener(e -> onCancel.run());

    return new HorizontalLayout(saveBtn, cancelBtn);
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private Span sectionLabel(String text) {
    Span label = new Span(text);
    label
        .getStyle()
        .set("font-size", "var(--lumo-font-size-s)")
        .set("font-weight", "600")
        .set("color", "var(--lumo-secondary-text-color)")
        .set("text-transform", "uppercase")
        .set("letter-spacing", "0.05em")
        .set("padding-top", "var(--lumo-space-s)")
        .set("display", "block");
    return label;
  }
}

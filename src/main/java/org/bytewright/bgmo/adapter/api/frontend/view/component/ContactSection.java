package org.bytewright.bgmo.adapter.api.frontend.view.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.bytewright.bgmo.adapter.api.frontend.view.component.factory.ComponentFactory;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.domain.service.notification.VerificationCodeService;
import org.bytewright.bgmo.usecases.UserWorkflows;

/**
 * Contact management section for ProfileView.
 *
 * <p>Messenger types (Telegram, Signal): one per type max, link-only via bot code dialog. Freeform
 * types (Email, Phone, Address): multiple allowed, inline add form. All existing entries: remove
 * only, no editing.
 */
public class ContactSection extends VerticalLayout {

  private final VerificationCodeService verificationService;
  private final UserWorkflows userWorkflows;
  private final RegisteredUserDao userDao;
  private final RegisteredUser currentUser;
  private final String telegramBotHandle;
  private final String signalBotHandle;

  public ContactSection(
      ComponentFactory componentFactory,
      VerificationCodeService verificationService,
      UserWorkflows userWorkflows,
      RegisteredUserDao userDao,
      RegisteredUser currentUser,
      Map<ContactInfoType, String> botHandles) {
    this.verificationService = verificationService;
    this.userWorkflows = userWorkflows;
    this.userDao = userDao;
    this.currentUser = currentUser;
    this.telegramBotHandle = botHandles.get(ContactInfoType.TELEGRAM);
    this.signalBotHandle = botHandles.get(ContactInfoType.SIGNAL);

    setPadding(false);
    setSpacing(false);

    rebuild();
  }

  private void rebuild() {
    removeAll();

    List<ContactInfo> contacts =
        currentUser.getContactInfos().stream()
            .sorted(Comparator.comparing(c -> c.type().name()))
            .toList();

    add(buildMessengerSection(contacts, ContactInfoType.TELEGRAM, telegramBotHandle));
    add(new Hr());
    add(buildMessengerSection(contacts, ContactInfoType.SIGNAL, signalBotHandle));
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

  private Component buildMessengerSection(
      List<ContactInfo> contacts, ContactInfoType type, String botHandle) {

    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.setSpacing(false);

    section.add(sectionLabel(messengerName(type)));

    contacts.stream()
        .filter(c -> c.type() == type)
        .findFirst()
        .ifPresentOrElse(
            linked -> section.add(buildLinkedMessengerRow(linked, type)),
            () -> section.add(buildLinkButton(type, botHandle)));

    return section;
  }

  private Component buildLinkedMessengerRow(ContactInfo contact, ContactInfoType type) {
    Span handle = new Span(displayValue(contact));
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
          userWorkflows.removeContact(currentUser.getId(), contact);
          currentUser.getContactInfos().remove(contact);
          rebuild();
          Notification n = Notification.show(messengerName(type) + " unlinked.");
          n.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
        });

    HorizontalLayout row = new HorizontalLayout(handle, verifiedBadge, unlinkBtn);
    row.setAlignItems(FlexComponent.Alignment.CENTER);
    row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
    row.setWidthFull();
    row.getStyle().set("padding", "var(--lumo-space-s) 0");
    return row;
  }

  private Component buildLinkButton(ContactInfoType type, String botHandle) {
    Button linkBtn =
        new Button("Link " + messengerName(type) + " account", VaadinIcon.LINK.create());
    linkBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    linkBtn.addClickListener(
        e -> {
          String code = verificationService.generateCode(currentUser.getId());
          MessengerLinkDialog dialog =
              new MessengerLinkDialog(
                  type,
                  code,
                  botHandle,
                  currentUser,
                  userDao,
                  () -> {
                    userDao
                        .find(currentUser.getId())
                        .ifPresent(
                            updated -> currentUser.setContactInfos(updated.getContactInfos()));
                    rebuild();
                    Notification n =
                        Notification.show(messengerName(type) + " linked successfully!");
                    n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                  });
          dialog.open();
        });
    linkBtn.getStyle().set("margin-top", "var(--lumo-space-xs)");
    return linkBtn;
  }

  // -------------------------------------------------------------------------
  // Freeform section (Email, Phone, Address — multiple allowed)
  // -------------------------------------------------------------------------

  private Component buildFreeformSection(List<ContactInfo> contacts, ContactInfoType type) {
    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.setSpacing(false);

    section.add(sectionLabel(labelFor(type)));

    // Existing entries
    contacts.stream().filter(c -> c.type() == type).forEach(c -> section.add(buildFreeformRow(c)));

    // Inline add form (hidden until button clicked)
    VerticalLayout addForm = buildAddForm(type);
    addForm.setVisible(false);

    Button addBtn = new Button("+ Add " + labelFor(type));
    addBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    addBtn.getStyle().set("margin-top", "var(--lumo-space-xs)");
    addBtn.addClickListener(
        e -> {
          addForm.setVisible(true);
          addBtn.setVisible(false);
        });

    section.add(addBtn, addForm);
    return section;
  }

  private Component buildFreeformRow(ContactInfo contact) {
    Span value = new Span(displayValue(contact));
    value.getStyle().set("font-size", "var(--lumo-font-size-s)").set("word-break", "break-all");

    Button removeBtn = new Button(VaadinIcon.TRASH.create());
    removeBtn.addThemeVariants(
        ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY,
        ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
    removeBtn.getElement().setAttribute("aria-label", "Remove");
    removeBtn.addClickListener(
        e -> {
          userWorkflows.removeContact(currentUser.getId(), contact);
          currentUser.getContactInfos().remove(contact);
          rebuild();
        });

    HorizontalLayout row = new HorizontalLayout(value, removeBtn);
    row.setAlignItems(FlexComponent.Alignment.CENTER);
    row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
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
        form.add(
            emailField,
            saveRow(
                () -> {
                  userWorkflows.addContactInfo(
                      currentUser.getId(),
                      ContactInfo.EmailContact.builder()
                          .userId(currentUser.getId())
                          .email(emailField.getValue())
                          .isVerified(false)
                          .build());
                  afterSave();
                },
                () -> form.setVisible(false)));
      }
      case PHONE -> {
        TextField phoneField = new TextField("Phone number");
        phoneField.setWidthFull();
        phoneField.setPlaceholder("+49 123 456789");
        form.add(
            phoneField,
            saveRow(
                () -> {
                  userWorkflows.addContactInfo(
                      currentUser.getId(),
                      ContactInfo.PhoneContact.builder()
                          .userId(currentUser.getId())
                          .phoneNr(phoneField.getValue())
                          .build());
                  afterSave();
                },
                () -> form.setVisible(false)));
      }
      case ADDRESS -> {
        TextField nameOnBell = new TextField("Name on bell");
        TextField street = new TextField("Street & Nr.");
        TextField zip = new TextField("ZIP");
        TextField city = new TextField("City");
        TextField comment = new TextField("Comment (optional)");
        for (var f : List.of(nameOnBell, street, zip, city, comment)) f.setWidthFull();
        form.add(
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
                          .userId(currentUser.getId())
                          .nameOnBell(nameOnBell.getValue())
                          .street(street.getValue())
                          .zipCode(zip.getValue())
                          .city(city.getValue())
                          .comment(comment.getValue())
                          .build());
                  afterSave();
                },
                () -> form.setVisible(false)));
      }
      default -> {
        /* messenger types never reach here */
      }
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

  private void afterSave() {
    userDao
        .find(currentUser.getId())
        .ifPresent(updated -> currentUser.setContactInfos(updated.getContactInfos()));
    rebuild();
    Notification n = Notification.show("Saved.");
    n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
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

  private static String displayValue(ContactInfo contact) {
    return switch (contact) {
      case ContactInfo.EmailContact e -> e.email();
      case ContactInfo.PhoneContact p -> p.phoneNr();
      case ContactInfo.TelegramContact t -> t.chatId();
      case ContactInfo.SignalContact s -> s.signalHandle();
      case ContactInfo.AddressContact a ->
          List.of(a.nameOnBell(), a.street(), a.zipCode() + " " + a.city()).stream()
              .filter(v -> v != null && !v.isBlank())
              .reduce((x, y) -> x + ", " + y)
              .orElse("—");
    };
  }

  private static String messengerName(ContactInfoType type) {
    return switch (type) {
      case TELEGRAM -> "Telegram";
      case SIGNAL -> "Signal";
      default -> type.name();
    };
  }

  private static String labelFor(ContactInfoType type) {
    return switch (type) {
      case EMAIL -> "Email";
      case PHONE -> "Phone";
      case ADDRESS -> "Address";
      default -> type.name();
    };
  }
}

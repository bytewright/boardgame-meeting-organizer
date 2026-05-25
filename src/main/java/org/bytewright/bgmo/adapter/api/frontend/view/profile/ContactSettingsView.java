package org.bytewright.bgmo.adapter.api.frontend.view.profile;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.bytewright.bgmo.adapter.api.frontend.service.SessionInfoService;
import org.bytewright.bgmo.adapter.api.frontend.view.LoginView;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.adapter.api.frontend.view.component.factory.ComponentFactory;
import org.bytewright.bgmo.domain.model.user.*;
import org.bytewright.bgmo.usecases.UserWorkflows;

@Route(value = "profile/contacts", layout = MainLayout.class)
@PageTitle("Contact Info | " + APP_NAME_SHORT)
@PermitAll
@RequiredArgsConstructor
public class ContactSettingsView extends VerticalLayout implements BeforeEnterObserver {

  private final SessionInfoService authService;
  private final ComponentFactory componentFactory;
  private final UserWorkflows userWorkflows;

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Optional<RegisteredUser> userOpt = authService.getCurrentUser();
    if (userOpt.isEmpty()) {
      event.forwardTo(LoginView.class);
      return;
    }
    buildView(userOpt.get());
  }

  private void rebuildView() {
    buildView(authService.getCurrentUser().orElseThrow());
  }

  private void buildView(RegisteredUser user) {
    removeAll();
    setAlignItems(Alignment.CENTER);
    setMaxWidth(MainLayout.MAX_DISPLAYPORT_WIDTH);
    getStyle().setMargin("0 auto");

    H2 title = new H2(getTranslation("profile.contacts.title"));

    add(title);
    Paragraph paragraph = new Paragraph(getTranslation("profile.contacts.intro"));
    paragraph.setWidthFull();
    add(paragraph);

    // Show the onboarding banner for users who have not yet added any contact info.
    // The banner disappears automatically on next navigation after PENDING_APPROVAL → ACTIVE.
    if (user.getStatus() == UserStatus.AFTER_REGISTRATION
        || user.resolvePrimaryContact().isEmpty()) {
      add(buildPendingBanner());
    } else {
      add(buildPrimaryContactSection(user));
    }

    ContactSection contactSection = componentFactory.contactSection(user, this::rebuildView);
    contactSection
        .getStyle()
        .set("max-width", MainLayout.MAX_DISPLAYPORT_WIDTH)
        .set("width", "100%");

    add(contactSection);
  }

  private Component buildPrimaryContactSection(RegisteredUser user) {
    VerticalLayout section = new VerticalLayout();
    section.setMaxWidth(MainLayout.MAX_DISPLAYPORT_WIDTH);
    section.setWidthFull();
    section.setPadding(true);
    section.setSpacing(true);
    section
        .getStyle()
        .set("background-color", "var(--lumo-contrast-5pct)")
        .set("border-radius", "var(--lumo-border-radius-m)")
        .set("margin-bottom", "var(--lumo-space-m)");

    H3 heading = new H3(getTranslation("profile.account.primary.title"));
    heading.getStyle().set("margin", "0 0 var(--lumo-space-s) 0");
    section.add(heading);

    List<ContactOption> filteredOptions =
        user.getContactOptions().stream()
            .filter(contactOption -> contactOption.getType().isCanBePrimary())
            .sorted(Comparator.comparing(contactInfo -> contactInfo.getType().ordinal()))
            .toList();
    ComboBox<ContactOption> comboBox = new ComboBox<>();
    comboBox.setWidthFull();
    comboBox.setItems(filteredOptions);
    filteredOptions.stream()
        .filter(contactInfo -> contactInfo.id().equals(user.resolvePrimaryContact()))
        .findAny()
        .ifPresent(comboBox::setValue);
    comboBox.setHelperText(getTranslation("profile.contacts.primary.helper"));
    comboBox.setItemLabelGenerator(this::generateComboBoxLabel);
    comboBox.addValueChangeListener(
        e -> {
          userWorkflows.changePrimaryContactInfo(user.getId(), e.getValue());
          Notification n = Notification.show("Primary updated.");
          n.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
          rebuildView();
        });
    section.add(comboBox);
    return section;
  }

  private String generateComboBoxLabel(ContactOption contactOption) {
    String label = ContactInfoLabelUtil.labelFor(contactOption.getType());
    return "%s: %s"
        .formatted(label, ContactInfoLabelUtil.displayValue(contactOption.getContactInfo()));
  }

  /**
   * Highlighted callout shown to newly registered users who have not yet added any contact info.
   * Explains why a contact is required and what will happen once one is added.
   */
  private Component buildPendingBanner() {
    Div banner = new Div();
    banner
        .getStyle()
        .set("background-color", "var(--lumo-primary-color-10pct)")
        .set("border-left", "4px solid var(--lumo-primary-color)")
        .set("border-radius", "var(--lumo-border-radius-m)")
        .set("padding", "var(--lumo-space-m)")
        .set("max-width", MainLayout.MAX_DISPLAYPORT_WIDTH)
        .set("width", "100%")
        .set("box-sizing", "border-box");

    Div iconTitleRow = new Div();
    iconTitleRow
        .getStyle()
        .set("display", "flex")
        .set("align-items", "center")
        .set("gap", "var(--lumo-space-s)");

    var icon = VaadinIcon.INFO_CIRCLE.create();
    icon.getStyle().set("color", "var(--lumo-primary-color)").set("flex-shrink", "0");

    H3 bannerTitle = new H3(getTranslation("profile.contacts.pending.title"));
    bannerTitle
        .getStyle()
        .set("margin", "0")
        .set("font-size", "var(--lumo-font-size-m)")
        .set("color", "var(--lumo-primary-text-color)");

    iconTitleRow.add(icon, bannerTitle);

    Paragraph bannerText = new Paragraph(getTranslation("profile.contacts.pending.text"));
    bannerText
        .getStyle()
        .set("margin", "var(--lumo-space-xs) 0 0 0")
        .set("font-size", "var(--lumo-font-size-s)")
        .set("color", "var(--lumo-secondary-text-color)");

    banner.add(iconTitleRow, bannerText);
    return banner;
  }
}

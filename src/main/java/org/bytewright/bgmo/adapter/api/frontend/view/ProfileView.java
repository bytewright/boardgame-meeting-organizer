package org.bytewright.bgmo.adapter.api.frontend.view;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.bytewright.bgmo.adapter.api.frontend.SessionAuthenticationService;
import org.bytewright.bgmo.adapter.api.frontend.view.component.ContactSection;
import org.bytewright.bgmo.adapter.api.frontend.view.component.GameLibSection;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.GameDao;
import org.bytewright.bgmo.domain.service.notification.VerificationCodeService;
import org.bytewright.bgmo.domain.service.security.PasswordRules;
import org.bytewright.bgmo.usecases.UserWorkflows;

@Route(value = "profile", layout = MainLayout.class)
@PageTitle("Profile | " + APP_NAME_SHORT)
@PermitAll
@RequiredArgsConstructor
public class ProfileView extends VerticalLayout implements BeforeEnterObserver {

  private final SessionAuthenticationService authService;
  private final UserWorkflows userWorkflows;
  private final VerificationCodeService verificationService;
  private final GameDao gameDao;

  private final VerticalLayout content = new VerticalLayout();
  private RegisteredUser currentUser;

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Optional<RegisteredUser> userOpt = authService.getCurrentUser();
    if (userOpt.isEmpty()) {
      event.forwardTo(LoginView.class);
      return;
    }
    this.currentUser = userOpt.get();
    refreshView();
  }

  private void refreshView() {
    removeAll();
    content.removeAll();
    setAlignItems(Alignment.CENTER);

    content.setMaxWidth(MainLayout.MAX_DISPLAYPORT_WIDTH);
    content.setWidthFull();

    add(new H2(getTranslation("profile.title")));
    add(createVerificationSection());
    add(createAccountSection());
    add(createContactSection());
    add(createLibrarySection());
    add(content);
  }

  private Component createVerificationSection() {
    String code = verificationService.generateCode(currentUser.getId());

    TextField codeField = new TextField(getTranslation("profile.verification.code"));
    codeField.setValue(code);
    codeField.setReadOnly(true);
    codeField.setWidthFull();

    // Copy to Clipboard logic
    Button copyBtn = new Button(VaadinIcon.COPY.create());
    copyBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    copyBtn.addClickListener(
        e -> {
          codeField.getElement().executeJs("window.navigator.clipboard.writeText($0)", code);
          Notification.show(getTranslation("profile.status.copied"));
        });
    codeField.setSuffixComponent(copyBtn);

    Span description = new Span(getTranslation("profile.verification.description", "@BGMO_Bot"));
    description.getStyle().set("font-size", "var(--lumo-font-size-s)");

    VerticalLayout layout = new VerticalLayout(description, codeField);
    return wrapInStyledSection(getTranslation("profile.verification.title"), layout);
  }

  private Component createAccountSection() {
    VerticalLayout layout = new VerticalLayout();
    layout.setSpacing(true);
    layout.setPadding(true);

    // --- Part A: General Info (Auto-save or distinct save) ---
    TextField nameField =
        new TextField(
            getTranslation("profile.account.displayName"), currentUser.getDisplayName(), "");
    nameField.setWidthFull();

    Button saveDisplayNameBtn =
        new Button(
            getTranslation("profile.action.save_displayName"),
            e -> {
              userWorkflows.changeDisplayName(currentUser.getId(), nameField.getValue());
              Notification.show(getTranslation("profile.status.saved"));
            });
    ComboBox<Locale> localePicker = new ComboBox<>(getTranslation("profile.account.locale"));
    localePicker.setItems(Locale.GERMAN, Locale.ENGLISH);
    localePicker.setItemLabelGenerator(l -> l.getDisplayLanguage(getLocale()));
    localePicker.setValue(
        currentUser.getPreferredLocale() != null ? currentUser.getPreferredLocale() : getLocale());
    localePicker.setWidthFull();
    localePicker.addValueChangeListener(
        e -> {
          userWorkflows.changeLocale(currentUser.getId(), localePicker.getValue());
          Notification.show(getTranslation("profile.status.saved"));
        });

    // --- Part B: Security (Password Change) ---
    PasswordField pwdField = new PasswordField(getTranslation("profile.account.password"));
    PasswordField confirmPwdField =
        new PasswordField(getTranslation("profile.account.confirm_password"));
    pwdField.setWidthFull();
    confirmPwdField.setWidthFull();

    Binder<RegisteredUser> pwdBinder = new Binder<>();

    // Cross-field validation for password matching
    pwdBinder
        .forField(pwdField)
        .asRequired(getTranslation("profile.error.pwd_required"))
        .withValidator(
            p -> p.length() >= PasswordRules.PW_MIN_CHARS,
            getTranslation("profile.error.pwd_too_short"))
        .bind(u -> "", (u, v) -> {}); // Dummy binding for validation only

    pwdBinder
        .forField(confirmPwdField)
        .withValidator(
            cp -> cp.equals(pwdField.getValue()), getTranslation("profile.error.pwd_mismatch"))
        .bind(u -> "", (u, v) -> {});

    Button updatePwdBtn =
        new Button(
            getTranslation("profile.action.update_password"),
            e -> {
              if (pwdBinder.validate().isOk()) {
                userWorkflows.changePassword(currentUser.getId(), pwdField.getValue());
                pwdField.clear();
                confirmPwdField.clear();
                Notification.show(getTranslation("profile.status.pwd_updated"));
              }
            });
    updatePwdBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    layout.add(
        nameField, saveDisplayNameBtn, localePicker, pwdField, confirmPwdField, updatePwdBtn);
    layout.setAlignItems(Alignment.END); // Align buttons to the right for better visual flow

    return wrapInStyledSection(getTranslation("profile.account.title"), layout);
  }

  private Component createContactSection() {
    ContactSection contactSection = new ContactSection(userWorkflows, currentUser);
    return wrapInStyledSection(getTranslation("profile.contacts.title"), contactSection);
  }

  private Component createLibrarySection() {
    GameLibSection gameLibSection =
        new GameLibSection(authService, userWorkflows, gameDao, currentUser);
    return wrapInStyledSection(getTranslation("profile.library.title"), gameLibSection);
  }

  private Component wrapInStyledSection(String title, Component content) {
    Details section = new Details(title, content);
    section.setWidthFull();
    section.setMaxWidth(MainLayout.MAX_DISPLAYPORT_WIDTH);

    // Styling the Header
    section
        .getElement()
        .getStyle()
        .set("background-color", "var(--lumo-contrast-5pct)")
        .set("border-radius", "var(--lumo-border-radius-m)")
        .set("margin-bottom", "var(--lumo-space-m)")
        .set("box-shadow", "var(--lumo-box-shadow-xs)");

    // Styling the Body (Targeting the content container specifically)
    content
        .getElement()
        .getStyle()
        .set("background-color", "var(--lumo-contrast-10pct)")
        .set("border-radius", "0 0 var(--lumo-border-radius-m) var(--lumo-border-radius-m)")
        .set("padding", "var(--lumo-space-m)");

    return section;
  }
}

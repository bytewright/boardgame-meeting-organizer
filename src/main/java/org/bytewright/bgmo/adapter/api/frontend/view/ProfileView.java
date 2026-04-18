package org.bytewright.bgmo.adapter.api.frontend.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
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
import org.bytewright.bgmo.usecases.UserWorkflows;

@Route(value = "profile", layout = MainLayout.class)
@PageTitle("Profile | BGMO")
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

    content.setMaxWidth("800px");
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

    Span description = new Span(getTranslation("profile.verification.description", "@BGMO_Bot"));
    description.getStyle().set("font-size", "var(--lumo-font-size-s)");

    VerticalLayout layout = new VerticalLayout(description, codeField);
    // TODO Add a "Copy to Clipboard" suffix button to the text field. Mobile users hate manually
    // highlighting and copying codes.
    return new Details(getTranslation("profile.verification.title"), layout);
  }

  private Component createAccountSection() {
    TextField nameField =
        new TextField(
            getTranslation("profile.account.displayName"), currentUser.getDisplayName(), "");
    PasswordField pwdField = new PasswordField(getTranslation("profile.account.password"));

    ComboBox<Locale> localePicker = new ComboBox<>(getTranslation("profile.account.locale"));
    localePicker.setItems(Locale.GERMAN, Locale.ENGLISH);
    localePicker.setItemLabelGenerator(l -> l.getDisplayLanguage(getLocale()));
    localePicker.setValue(getLocale());

    Button saveBtn =
        new Button(
            getTranslation("profile.action.save"),
            e -> {
              userWorkflows.changeDisplayName(currentUser.getId(), nameField.getValue());
              if (!pwdField.isEmpty()) {
                userWorkflows.changePassword(currentUser.getId(), pwdField.getValue());
              }
              userWorkflows.changeLocale(currentUser.getId(), localePicker.getValue());
              Notification.show(getTranslation("profile.status.saved"));
            });
    saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    VerticalLayout layout = new VerticalLayout(nameField, pwdField, localePicker, saveBtn);
    return new Details(getTranslation("profile.account.title"), layout);
  }

  private Component createContactSection() {
    ContactSection contactSection = new ContactSection(userWorkflows, currentUser);
    return new Details(getTranslation("profile.contacts.title"), contactSection);
  }

  private Component createLibrarySection() {
    GameLibSection gameLibSection =
        new GameLibSection(authService, userWorkflows, gameDao, currentUser);
    return new Details(getTranslation("profile.library.title"), gameLibSection);
  }
}

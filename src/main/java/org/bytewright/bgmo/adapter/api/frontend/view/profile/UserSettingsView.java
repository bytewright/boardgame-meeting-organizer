package org.bytewright.bgmo.adapter.api.frontend.view.profile;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
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
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.bytewright.bgmo.adapter.api.frontend.SessionAuthenticationService;
import org.bytewright.bgmo.adapter.api.frontend.view.LoginView;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.security.PasswordRules;
import org.bytewright.bgmo.usecases.UserWorkflows;

@Route(value = "profile/settings", layout = MainLayout.class)
@PageTitle("Account Settings | " + APP_NAME_SHORT)
@PermitAll
@RequiredArgsConstructor
public class UserSettingsView extends VerticalLayout implements BeforeEnterObserver {

  private final SessionAuthenticationService authService;
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

  private void buildView(RegisteredUser user) {
    removeAll();
    setAlignItems(Alignment.CENTER);

    setMaxWidth(MainLayout.MAX_DISPLAYPORT_WIDTH);
    getStyle().setMargin("0 auto");

    H2 title = new H2(getTranslation("profile.account.title"));

    add(title, buildDisplayNameSection(user), buildPasswordSection(user));
  }

  // -------------------------------------------------------------------------
  // Display name
  // -------------------------------------------------------------------------

  private VerticalLayout buildDisplayNameSection(RegisteredUser user) {
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

    H3 heading = new H3(getTranslation("profile.account.displayName"));
    heading.getStyle().set("margin", "0 0 var(--lumo-space-s) 0");

    TextField nameField =
        new TextField(getTranslation("profile.account.displayName"), user.getDisplayName(), "");
    nameField.setWidthFull();

    Button saveBtn =
        new Button(
            getTranslation("profile.action.save_displayName"),
            e -> {
              userWorkflows.changeDisplayName(user.getId(), nameField.getValue());
              Notification.show(getTranslation("profile.status.saved"));
            });
    saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    section.add(heading, nameField, saveBtn);
    return section;
  }

  // -------------------------------------------------------------------------
  // Password change
  // -------------------------------------------------------------------------

  private VerticalLayout buildPasswordSection(RegisteredUser user) {
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

    H3 heading = new H3(getTranslation("profile.account.password"));
    heading.getStyle().set("margin", "0 0 var(--lumo-space-s) 0");

    PasswordField pwdField = new PasswordField(getTranslation("profile.account.password"));
    PasswordField confirmPwdField =
        new PasswordField(getTranslation("profile.account.confirm_password"));
    pwdField.setWidthFull();
    confirmPwdField.setWidthFull();

    Binder<RegisteredUser> pwdBinder = new Binder<>();
    pwdBinder
        .forField(pwdField)
        .asRequired(getTranslation("profile.error.pwd_required"))
        .withValidator(
            p -> p.length() >= PasswordRules.PW_MIN_CHARS,
            getTranslation("profile.error.pwd_too_short"))
        .bind(u -> "", (u, v) -> {});

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
                userWorkflows.changePassword(user.getId(), pwdField.getValue());
                pwdField.clear();
                confirmPwdField.clear();
                Notification.show(getTranslation("profile.status.pwd_updated"));
              }
            });
    updatePwdBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    section.add(heading, pwdField, confirmPwdField, updatePwdBtn);
    return section;
  }
}

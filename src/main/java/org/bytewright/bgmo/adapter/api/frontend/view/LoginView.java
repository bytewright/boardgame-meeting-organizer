package org.bytewright.bgmo.adapter.api.frontend.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.api.frontend.SessionAuthenticationService;

@Slf4j
@Route("login")
@PageTitle("Login")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

  private final SessionAuthenticationService authService;
  private final LoginForm loginForm = new LoginForm();

  public LoginView(SessionAuthenticationService authService) {
    this.authService = authService;

    addClassName("login-view");
    setSizeFull();
    setAlignItems(Alignment.CENTER);
    setJustifyContentMode(JustifyContentMode.CENTER);

    loginForm.setAction("login"); // Vaadin handles this internally if set
    loginForm.addForgotPasswordListener(event -> authService.passwordReset());
    loginForm.addLoginListener(
        event -> {
          boolean isAuthenticated = authService.login(event.getUsername(), event.getPassword());
          if (isAuthenticated) {
            Notification.show(
                getTranslation("login.success"), 3000, Notification.Position.TOP_CENTER);
            UI.getCurrent().navigate(DashboardView.class);
          } else {
            loginForm.setError(true);
          }
        });

    Button registerButton = new Button(getTranslation("login.register"));
    registerButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    registerButton.addClickListener(e -> Notification.show("Registration is not yet implemented."));

    add(new H1(getTranslation("login.title")), loginForm, registerButton);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    // If the user is already authenticated, redirect to the dashboard
    if (authService.getCurrentUser().isPresent()) {
      event.forwardTo(DashboardView.class);
    }
  }
}

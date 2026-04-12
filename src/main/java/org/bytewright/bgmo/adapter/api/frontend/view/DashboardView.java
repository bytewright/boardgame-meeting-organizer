package org.bytewright.bgmo.adapter.api.frontend.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.PermitAll;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.api.frontend.SessionAuthenticationService;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;

@Slf4j
@Route("")
@RouteAlias("dashboard")
@RouteAlias("home")
@PageTitle("Dashboard | Boardgame Meeting Organizer")
@PermitAll // Requires security configuration, or manually check auth
public class DashboardView extends VerticalLayout implements BeforeEnterObserver {
  private final SessionAuthenticationService authService;
  private RegisteredUser currentUser;

  public DashboardView(SessionAuthenticationService authService) {
    this.authService = authService;

    setSizeFull();
    setPadding(true);
    setSpacing(true);
  }

  private void buildUI() {
    removeAll(); // Clear previous UI if any (e.g., if re-entering)

    H2 welcomeHeader = new H2("Welcome, " + currentUser.getName() + "!");
    Button logoutButton =
        new Button(
            "Logout",
            e -> {
              authService.logout();
              UI.getCurrent().navigate(LoginView.class);
            });
    HorizontalLayout headerLayout = new HorizontalLayout(welcomeHeader, logoutButton);
    headerLayout.setAlignItems(Alignment.BASELINE);
    headerLayout.setFlexGrow(1, welcomeHeader);

    add(headerLayout);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Optional<RegisteredUser> userOpt = authService.getCurrentUser();
    if (userOpt.isEmpty()) {
      log.debug("User is not logged in, redirecting to log-in view");
      // Not authenticated, redirect to log-in
      event.forwardTo(LoginView.class);
      return;
    }
    this.currentUser = userOpt.get();
    buildUI();
  }
}

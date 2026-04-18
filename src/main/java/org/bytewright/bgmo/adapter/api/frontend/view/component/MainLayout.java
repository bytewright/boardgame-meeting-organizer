package org.bytewright.bgmo.adapter.api.frontend.view.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;
import org.bytewright.bgmo.adapter.api.frontend.SessionAuthenticationService;
import org.bytewright.bgmo.adapter.api.frontend.view.*;
import org.bytewright.bgmo.domain.model.user.UserRole;
import org.bytewright.bgmo.domain.service.data.GameDao;
import org.bytewright.bgmo.usecases.MeetupWorkflows;

public class MainLayout extends AppLayout implements RouterLayout {
  public static final String MAX_DISPLAYPORT_WIDTH = "800px"; // Mobile-first reasonable fixed width

  private final SessionAuthenticationService authService;
  private final MeetupWorkflows meetupWorkflows;
  private final GameDao gameDao;

  public MainLayout(
      SessionAuthenticationService authService, MeetupWorkflows meetupWorkflows, GameDao gameDao) {
    this.authService = authService;
    this.meetupWorkflows = meetupWorkflows;
    this.gameDao = gameDao;

    createHeader();
  }

  private void createHeader() {
    // Left Side: Back/Home link
    RouterLink homeLink = new RouterLink("", DashboardView.class);
    homeLink.add(VaadinIcon.CHEVRON_LEFT.create());
    homeLink.getStyle().set("text-decoration", "none").set("color", "inherit");

    // Title or Logo
    H2 logo = new H2("BGMO");
    logo.getStyle()
        .set("font-size", "var(--lumo-font-size-l)")
        .set("margin", "0 var(--lumo-space-m)");

    // Right Side: Action Buttons
    Button createBtn = new Button("New Meetup", VaadinIcon.PLUS.create(), e -> openCreateDialog());
    createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

    Button libBtn =
        new Button(
            "Profile",
            VaadinIcon.HANDS_UP.create(),
            e -> UI.getCurrent().navigate(ProfileView.class));
    libBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    Button logoutBtn =
        new Button(
            "logout",
            VaadinIcon.EXIT.create(),
            e -> {
              authService.logout();
              UI.getCurrent().navigate(LoginView.class);
            });
    logoutBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

    HorizontalLayout header = new HorizontalLayout(homeLink, logo, libBtn, createBtn, logoutBtn);
    if (authService
        .getCurrentUser()
        .map(registeredUser -> registeredUser.getRole() == UserRole.ADMIN)
        .orElse(false)) {
      header.add(adminLink());
    }
    header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
    header.expand(logo); // Pushes buttons to the right
    header.setWidthFull();
    header.setPadding(true);
    header.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

    addToNavbar(header);
  }

  private Component adminLink() {
    Button adminBtn =
        new Button(
            "Admin",
            VaadinIcon.CONTROLLER.create(),
            e -> UI.getCurrent().navigate(AdminUserApprovalView.class));
    adminBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    return adminBtn;
  }

  private void openCreateDialog() {
    authService
        .getCurrentUser()
        .ifPresent(
            user -> {
              // Note: Since we are in a layout, we might not want to force a refresh
              // of the specific child view unless it implements a refresh interface.
              new MeetupCreateDialog(
                      user,
                      meetupWorkflows,
                      gameDao,
                      () -> {
                        // Logic to trigger UI refresh if needed
                        UI.getCurrent().getPage().reload();
                      })
                  .open();
            });
  }
}

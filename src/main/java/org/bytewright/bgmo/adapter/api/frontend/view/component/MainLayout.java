package org.bytewright.bgmo.adapter.api.frontend.view.component;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.time.Clock;
import org.bytewright.bgmo.adapter.api.frontend.SessionAuthenticationService;
import org.bytewright.bgmo.adapter.api.frontend.view.*;
import org.bytewright.bgmo.adapter.api.frontend.view.admin.AdminDashboardView;
import org.bytewright.bgmo.adapter.api.frontend.view.legal.DatenschutzView;
import org.bytewright.bgmo.adapter.api.frontend.view.legal.ImpressumView;
import org.bytewright.bgmo.adapter.api.frontend.view.legal.TermsOfUseView;
import org.bytewright.bgmo.adapter.api.frontend.view.meetup.MeetupCreateDialog;
import org.bytewright.bgmo.adapter.api.frontend.view.profile.ProfileView;
import org.bytewright.bgmo.domain.service.data.GameDao;
import org.bytewright.bgmo.usecases.MeetupWorkflows;

@AnonymousAllowed
public class MainLayout extends AppLayout implements RouterLayout {
  public static final String MAX_DISPLAYPORT_WIDTH = "800px"; // Mobile-first reasonable fixed width

  private final SessionAuthenticationService authService;
  private final MeetupWorkflows meetupWorkflows;
  private final GameDao gameDao;
  private final Clock clock;

  public MainLayout(
      SessionAuthenticationService authService,
      MeetupWorkflows meetupWorkflows,
      GameDao gameDao,
      Clock clock) {
    this.authService = authService;
    this.meetupWorkflows = meetupWorkflows;
    this.gameDao = gameDao;
    this.clock = clock;

    createHeader();
  }

  private void createHeader() {
    // Logo — clicking always navigates to dashboard
    RouterLink logoLink = new RouterLink("", DashboardView.class);
    H2 logo = new H2(APP_NAME_SHORT);
    logo.getStyle()
        .set("font-size", "var(--lumo-font-size-l)")
        .set("margin", "0 var(--lumo-space-s)");
    logoLink.add(logo);
    logoLink.getStyle().set("text-decoration", "none").set("color", "inherit");

    boolean loggedIn = authService.getCurrentUser().isPresent();

    HorizontalLayout header;
    if (loggedIn) {
      header = buildAuthenticatedHeader(logoLink);
    } else {
      header = buildAnonymousHeader(logoLink);
    }

    header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
    header.expand(logoLink); // Logo takes remaining space, buttons stay right-aligned
    header.setWidthFull();
    header.setPadding(true);
    header.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

    addToNavbar(header);
  }

  /** Header for logged-in users: Profile, Create, (Admin,) Logout. */
  private HorizontalLayout buildAuthenticatedHeader(RouterLink logoLink) {
    Button profileBtn =
        createNavButton(
            getTranslation("navbar.profile"),
            VaadinIcon.HANDS_UP.create(),
            e -> UI.getCurrent().navigate(ProfileView.class));
    profileBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    Button createBtn =
        createNavButton(
            getTranslation("navbar.create-meetup"),
            VaadinIcon.PLUS.create(),
            e -> openCreateDialog());
    createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

    Button logoutBtn =
        createNavButton(
            getTranslation("navbar.logout"),
            VaadinIcon.EXIT.create(),
            e -> {
              authService.logout();
              UI.getCurrent().navigate(LoginView.class);
            });
    logoutBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

    HorizontalLayout header = new HorizontalLayout(logoLink, profileBtn, createBtn, logoutBtn);
    if (authService.isCurrentUserAdmin()) {
      header.add(buildAdminButton());
    }
    return header;
  }

  /** Header for anonymous visitors: just a Login button */
  private HorizontalLayout buildAnonymousHeader(RouterLink logoLink) {
    Button loginBtn =
        createNavButton(
            getTranslation("navbar.login"),
            VaadinIcon.SIGN_IN.create(),
            e -> UI.getCurrent().navigate(LoginView.class));
    loginBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    return new HorizontalLayout(logoLink, loginBtn);
  }

  private Button createNavButton(
      String label, Icon icon, ComponentEventListener<ClickEvent<Button>> listener) {
    icon.getStyle().set("margin", "0"); // neutralise default icon margins inside buttons
    Button btn = new Button(label, icon, listener);
    btn.getStyle()
        .set("flex-direction", "column")
        .set("height", "auto")
        .set("min-height", "52px")
        .set("gap", "2px");
    return btn;
  }

  private Component buildAdminButton() {
    Button adminBtn =
        createNavButton(
            "Admin",
            VaadinIcon.CONTROLLER.create(),
            e -> UI.getCurrent().navigate(AdminDashboardView.class));
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
                      clock,
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

  @Override
  public void showRouterLayoutContent(HasElement content) {
    Component target = null;
    if (content != null) {
      target =
          content
              .getElement()
              .getComponent()
              .orElseThrow(
                  () -> new IllegalArgumentException("AppLayout content must be a Component"));
    }
    VerticalLayout wrapper = new VerticalLayout();
    wrapper.setSizeFull();
    wrapper.setPadding(false);
    wrapper.setSpacing(false);

    wrapper.addAndExpand(target);

    wrapper.add(createFooter());
    setContent(wrapper);
  }

  private Component createFooter() {
    RouterLink impressum = new RouterLink("Impressum", ImpressumView.class);
    RouterLink datenschutz = new RouterLink("Datenschutz", DatenschutzView.class);
    RouterLink tos = new RouterLink("Nutzungsbedingungen", TermsOfUseView.class);

    HorizontalLayout footer =
        new HorizontalLayout(impressum, new Span("·"), datenschutz, new Span("·"), tos);
    footer.setWidthFull();
    footer.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    footer
        .getStyle()
        .set("border-top", "1px solid var(--lumo-contrast-10pct)")
        .set("padding", "var(--lumo-space-s)")
        .set("font-size", "var(--lumo-font-size-s)")
        .set("color", "var(--lumo-secondary-text-color)");
    return footer;
  }
}

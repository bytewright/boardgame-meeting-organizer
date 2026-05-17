package org.bytewright.bgmo.adapter.api.frontend.view.component;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.bytewright.bgmo.adapter.api.frontend.service.SessionInfoService;
import org.bytewright.bgmo.adapter.api.frontend.view.*;
import org.bytewright.bgmo.adapter.api.frontend.view.admin.AdminDashboardView;
import org.bytewright.bgmo.adapter.api.frontend.view.component.factory.ComponentFactory;
import org.bytewright.bgmo.adapter.api.frontend.view.legal.AboutSiteView;
import org.bytewright.bgmo.adapter.api.frontend.view.legal.PrivacyPolicyView;
import org.bytewright.bgmo.adapter.api.frontend.view.legal.TermsOfUseView;
import org.bytewright.bgmo.adapter.api.frontend.view.meetup.MeetupCreationView;
import org.bytewright.bgmo.adapter.api.frontend.view.profile.ContactSettingsView;
import org.bytewright.bgmo.adapter.api.frontend.view.profile.GameLibView;
import org.bytewright.bgmo.adapter.api.frontend.view.profile.UserSettingsView;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;

@AnonymousAllowed
public class MainLayout extends AppLayout implements RouterLayout {
  public static final String MAX_DISPLAYPORT_WIDTH = "800px"; // Mobile-first reasonable fixed width
  private final ComponentFactory componentFactory;
  private final SessionInfoService authService;

  public MainLayout(ComponentFactory componentFactory, SessionInfoService authService) {
    this.componentFactory = componentFactory;
    this.authService = authService;

    createHeader();
  }

  private void createHeader() {
    // Logo — clicking always navigates to dashboard
    RouterLink logoLink = new RouterLink("", DashboardView.class);
    Image banner = new Image("assets/images/banner.png", "Site banner");
    banner.setHeight(75, Unit.PIXELS);
    logoLink.add(banner);
    logoLink.getStyle().set("text-decoration", "none").set("color", "inherit").setMarginTop("8px");
    logoLink.getElement().setAttribute("aria-label", "Go to dashboard");
    logoLink.setHighlightCondition((routerLink, event) -> false);

    HorizontalLayout header = new HorizontalLayout(logoLink);
    Component[] components =
        authService
            .getCurrentUser()
            .map(this::buildAuthenticatedHeader)
            .orElse(buildAnonymousHeader());
    header.add(components);

    header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
    header.expand(logoLink); // Logo takes remaining space, buttons stay right-aligned
    header.setMaxWidth(MAX_DISPLAYPORT_WIDTH);
    header.setWidthFull();
    header.setPadding(false);
    header
        .getStyle()
        .set("margin", "0 auto")
        .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

    addToNavbar(header);
  }

  /**
   * Header for logged-in users: Logo | [+ New Meetup] [👤▾] All secondary navigation (profile,
   * library, contacts, logout, admin) lives in the profile menu.
   */
  private Component[] buildAuthenticatedHeader(RegisteredUser user) {
    Button createBtn =
        createNavButton(
            getTranslation("navbar.create-meetup"),
            VaadinIcon.PLUS.create(),
            e -> UI.getCurrent().navigate(MeetupCreationView.class));
    createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
    MenuBar profileMenu = buildProfileMenuBar();
    return new Component[] {createBtn, profileMenu};
  }

  /** Header for anonymous visitors: Logo | [LocalePicker] [Login] */
  private Component[] buildAnonymousHeader() {
    Button loginBtn =
        createNavButton(
            getTranslation("navbar.login"),
            VaadinIcon.SIGN_IN.create(),
            e -> UI.getCurrent().navigate(LoginView.class));
    loginBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    LocalePicker localePicker = componentFactory.localePicker();
    return new Component[] {localePicker, loginBtn};
  }

  /**
   * Profile dropdown (👤 icon button).
   *
   * <p>Order: Logout · — · Game Library · Account Settings · Contact Info · [— · Admin]
   */
  private MenuBar buildProfileMenuBar() {
    MenuBar menuBar = new MenuBar();
    // Drop LUMO_ICON so the item isn't stripped of padding;
    // LUMO_TERTIARY keeps it ghost-style but we add our own tint below.
    menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY);
    menuBar
        .getStyle()
        .set("background-color", "var(--lumo-contrast-10pct)")
        .set("border-radius", "var(--lumo-border-radius-m)");

    // --- Trigger: person icon + down chevron ---
    Icon userIcon = VaadinIcon.USER.create();
    userIcon
        .getStyle()
        .set("margin", "0")
        .set("width", "var(--lumo-icon-size-s)")
        .set("height", "var(--lumo-icon-size-s)");

    Icon chevron = VaadinIcon.ANGLE_DOWN.create();
    chevron
        .getStyle()
        .set("margin", "0")
        .set("width", "var(--lumo-icon-size-s)")
        .set("height", "var(--lumo-icon-size-s)")
        .set("color", "var(--lumo-secondary-text-color)");

    HorizontalLayout trigger = new HorizontalLayout(userIcon, chevron);
    trigger.setAlignItems(FlexComponent.Alignment.CENTER);
    trigger.setSpacing(false);
    trigger.getStyle().set("gap", "var(--lumo-space-xs)");

    MenuItem root = menuBar.addItem(trigger);
    SubMenu sub = root.getSubMenu();

    // --- Logout first (users who need it should find it quickly) ---
    sub.addItem(
        menuItemContent(VaadinIcon.EXIT, getTranslation("navbar.logout")),
        e -> {
          authService.logout();
          UI.getCurrent().navigate(LoginView.class);
        });

    sub.addComponent(new Hr());

    // --- Navigation items ---
    sub.addItem(
        menuItemContent(VaadinIcon.GAMEPAD, getTranslation("navbar.game-library")),
        e -> UI.getCurrent().navigate(GameLibView.class));

    sub.addItem(
        menuItemContent(VaadinIcon.COG, getTranslation("navbar.account-settings")),
        e -> UI.getCurrent().navigate(UserSettingsView.class));

    sub.addItem(
        menuItemContent(VaadinIcon.ENVELOPE, getTranslation("navbar.contact-settings")),
        e -> UI.getCurrent().navigate(ContactSettingsView.class));

    // --- Admin (conditional) ---
    if (authService.isCurrentUserAdmin()) {
      sub.addComponent(new Hr());
      sub.addItem(
          menuItemContent(VaadinIcon.CONTROLLER, "Admin"),
          e -> UI.getCurrent().navigate(AdminDashboardView.class));
    }

    return menuBar;
  }

  /** Builds a labelled menu item row with a leading icon and a text label. */
  private Component menuItemContent(VaadinIcon iconEnum, String label) {
    Icon icon = iconEnum.create();
    icon.getStyle()
        .set("color", "var(--lumo-secondary-text-color)")
        .set("width", "var(--lumo-icon-size-s)")
        .set("height", "var(--lumo-icon-size-s)");

    Span text = new Span(label);

    HorizontalLayout row = new HorizontalLayout(icon, text);
    row.setAlignItems(FlexComponent.Alignment.CENTER);
    row.setSpacing(false);
    row.getStyle().set("gap", "var(--lumo-space-s)").set("padding", "var(--lumo-space-xs) 0");
    return row;
  }

  private Button createNavButton(
      String label, Icon icon, ComponentEventListener<ClickEvent<Button>> listener) {
    icon.getStyle().set("margin", "0");
    Button btn = new Button(label, icon, listener);
    btn.getStyle()
        .set("flex-direction", "column")
        .set("height", "auto")
        .set("min-height", "52px")
        .set("gap", "2px");
    return btn;
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
    wrapper.setWidthFull();
    wrapper.setMinHeight(100, Unit.PERCENTAGE);
    wrapper.setPadding(false);
    wrapper.setSpacing(false);

    wrapper.addAndExpand(target);
    wrapper.add(createFooter());
    setContent(wrapper);
  }

  /** Footer with legal links and the locale picker */
  private Component createFooter() {
    RouterLink impressum =
        new RouterLink(getTranslation("main.footer.about-site"), AboutSiteView.class);
    RouterLink datenschutz =
        new RouterLink(getTranslation("main.footer.privacy-policy"), PrivacyPolicyView.class);
    RouterLink tos =
        new RouterLink(getTranslation("main.footer.terms-of-service"), TermsOfUseView.class);
    LocalePicker localePicker = componentFactory.localePicker();

    HorizontalLayout footer =
        new HorizontalLayout(
            impressum, new Span("·"), datenschutz, new Span("·"), tos, new Span("·"), localePicker);
    footer.setWidthFull();
    footer.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    footer.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
    footer
        .getStyle()
        .set("border-top", "1px solid var(--lumo-contrast-10pct)")
        .set("font-size", "var(--lumo-font-size-s)");
    return footer;
  }
}

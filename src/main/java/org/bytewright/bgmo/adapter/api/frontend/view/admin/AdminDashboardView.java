package org.bytewright.bgmo.adapter.api.frontend.view.admin;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.Optional;
import org.bytewright.bgmo.adapter.api.frontend.SessionAuthenticationService;
import org.bytewright.bgmo.adapter.api.frontend.view.LoginView;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.usecases.AdminWorkflows;

@Route(value = "admin", layout = MainLayout.class)
@PageTitle("Admin | " + APP_NAME_SHORT)
@RolesAllowed("ADMIN")
public class AdminDashboardView extends VerticalLayout implements BeforeEnterObserver {
  private final SessionAuthenticationService authService;

  public AdminDashboardView(
      SessionAuthenticationService authService, AdminWorkflows adminWorkflows) {
    this.authService = authService;
    setMaxWidth(MainLayout.MAX_DISPLAYPORT_WIDTH);
    getStyle().set("margin", "0 auto");
    setPadding(true);
    setSpacing(true);

    add(new H1("Administration"));

    long pendingCount = adminWorkflows.listNonActive().size();

    HorizontalLayout cards = new HorizontalLayout();
    cards.setWidthFull();
    cards.setSpacing(true);
    cards.add(
        navCard(
            VaadinIcon.USER_CHECK,
            "Nutzer-Freischaltungen",
            pendingCount > 0 ? pendingCount + " ausstehende Anfrage(n)" : "Keine offenen Anfragen",
            pendingCount > 0,
            () -> UI.getCurrent().navigate(AdminUserApprovalView.class)),
        navCard(
            VaadinIcon.USER_CARD,
            "Nutzerverwaltung",
            "Nutzer editieren und sperren",
            false,
            () -> UI.getCurrent().navigate(AdminUserManagementView.class)),
        navCard(
            VaadinIcon.COG,
            "Site-Einstellungen",
            "Impressum-Daten und Adapter-Konfigurationen verwalten",
            false,
            () -> UI.getCurrent().navigate(AdminSiteSettingsView.class)));

    add(cards);
  }

  /**
   * A clickable card that navigates to a sub-page.
   *
   * @param icon icon to display prominently
   * @param title card heading
   * @param subtitle secondary description or status line
   * @param highlighted whether to draw attention (e.g. pending items exist)
   * @param onClick navigation action
   */
  private Div navCard(
      VaadinIcon icon, String title, String subtitle, boolean highlighted, Runnable onClick) {

    Icon ic = icon.create();
    ic.setSize("2em");
    ic.getStyle()
        .set("color", highlighted ? "var(--lumo-error-color)" : "var(--lumo-primary-color)");

    H3 heading = new H3(title);
    heading.getStyle().set("margin", "0");

    Paragraph desc = new Paragraph(subtitle);
    desc.getStyle()
        .set("margin", "0")
        .set("font-size", "var(--lumo-font-size-s)")
        .set("color", highlighted ? "var(--lumo-error-color)" : "var(--lumo-secondary-text-color)");

    VerticalLayout text = new VerticalLayout(heading, desc);
    text.setPadding(false);
    text.setSpacing(false);

    Div card = new Div(ic, text);
    card.getStyle()
        .set("display", "flex")
        .set("flex-direction", "column")
        .set("gap", "var(--lumo-space-m)")
        .set("padding", "var(--lumo-space-l)")
        .set(
            "border",
            "2px solid " + (highlighted ? "var(--lumo-error-color)" : "var(--lumo-contrast-20pct)"))
        .set("border-radius", "var(--lumo-border-radius-l)")
        .set("cursor", "pointer")
        .set("flex", "1")
        .set("transition", "background-color 0.15s ease");

    card.getElement()
        .addEventListener(
            "mouseover", e -> card.getStyle().set("background-color", "var(--lumo-contrast-5pct)"));
    card.getElement().addEventListener("mouseout", e -> card.getStyle().remove("background-color"));
    card.addClickListener(e -> onClick.run());

    return card;
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Optional<RegisteredUser> userOpt = authService.getCurrentUser(); //
    if (userOpt.isEmpty()) {
      event.forwardTo(LoginView.class);
    }
  }
}

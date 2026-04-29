package org.bytewright.bgmo.adapter.api.frontend.view.admin;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
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
import org.bytewright.bgmo.adapter.api.frontend.view.component.NavCardComponent;
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
        new NavCardComponent(
            VaadinIcon.USER_CHECK,
            getTranslation("admin-dashboard.nav.user-approval.title"),
            pendingCount > 0
                ? getTranslation("admin-dashboard.nav.user-approval.subtitle", pendingCount)
                : getTranslation("admin-dashboard.nav.user-approval.subtitle-none"),
            pendingCount > 0,
            () -> UI.getCurrent().navigate(AdminUserApprovalView.class)),
        new NavCardComponent(
            VaadinIcon.USER_CARD,
            getTranslation("admin-dashboard.nav.user-mngt.title"),
            getTranslation("admin-dashboard.nav.user-mngt.subtitle"),
            false,
            () -> UI.getCurrent().navigate(AdminUserManagementView.class)),
        new NavCardComponent(
            VaadinIcon.COG,
            getTranslation("admin-dashboard.nav.site-mngt.title"),
            getTranslation("admin-dashboard.nav.site-mngt.subtitle"),
            false,
            () -> UI.getCurrent().navigate(AdminSiteSettingsView.class)));
    add(cards);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Optional<RegisteredUser> userOpt = authService.getCurrentUser();
    if (userOpt.isEmpty()) {
      event.forwardTo(LoginView.class);
    }
  }
}

package org.bytewright.bgmo.adapter.api.frontend.view.admin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.Optional;
import org.bytewright.bgmo.adapter.api.frontend.SessionAuthenticationService;
import org.bytewright.bgmo.adapter.api.frontend.view.LoginView;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.usecases.AdminWorkflows;

@Route(value = "admin/approvals", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class AdminUserApprovalView extends VerticalLayout implements BeforeEnterObserver {
  private final SessionAuthenticationService authService;
  private final AdminWorkflows adminWorkflows;
  private RegisteredUser currentUser;
  private final Grid<RegisteredUser> grid = new Grid<>(RegisteredUser.class, false);

  public AdminUserApprovalView(
      SessionAuthenticationService authService, AdminWorkflows adminWorkflows) {
    this.authService = authService;
    this.adminWorkflows = adminWorkflows;

    add(new H2("Pending Registrations"));

    grid.addColumn(RegisteredUser::getDisplayName).setHeader("Name");
    grid.addColumn(RegisteredUser::getLoginName).setHeader("Username");
    grid.addComponentColumn(
            user -> {
              Button approveBtn =
                  new Button(
                      "Approve",
                      e -> {
                        adminWorkflows.approveUser(currentUser.getId(), user.getId());
                        refreshGrid();
                      });
              approveBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
              return approveBtn;
            })
        .setHeader("Actions");
    grid.setItemDetailsRenderer(new ComponentRenderer<>(this::renderUser));
    add(grid);
  }

  private Component renderUser(RegisteredUser user) {
    VerticalLayout layout = new VerticalLayout();
    layout.setPadding(true);
    layout.getStyle().set("background-color", "var(--lumo-contrast-5pct)");

    String introText = adminWorkflows.getRegistrationIntroText(user.getId());
    Paragraph text = new Paragraph(introText);
    text.getStyle().set("white-space", "pre-wrap"); // Preserves newlines

    layout.add(new H2("Intro text"), text);
    return layout;
  }

  private void refreshGrid() {
    grid.setItems(adminWorkflows.listNonActive());
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Optional<RegisteredUser> userOpt = authService.getCurrentUser(); //
    if (userOpt.isEmpty()) {
      event.forwardTo(LoginView.class);
      return;
    }
    this.currentUser = userOpt.get();
    refreshGrid();
  }
}

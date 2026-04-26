package org.bytewright.bgmo.adapter.api.frontend.view;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.bytewright.bgmo.adapter.api.frontend.service.i18n.LocaleService;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.model.user.UserStatus;
import org.bytewright.bgmo.usecases.AdminWorkflows;

@Route(value = "admin/users", layout = MainLayout.class)
@PageTitle("User management | " + APP_NAME_SHORT)
@RolesAllowed("ADMIN")
public class AdminUserManagementView extends VerticalLayout {
  private final LocaleService localeService;
  private final AdminWorkflows adminWorkflows;
  private final Grid<RegisteredUser> grid = new Grid<>(RegisteredUser.class, false);

  public AdminUserManagementView(LocaleService localeService, AdminWorkflows adminWorkflows) {
    this.localeService = localeService;
    this.adminWorkflows = adminWorkflows;

    setMaxWidth(MainLayout.MAX_DISPLAYPORT_WIDTH);
    getStyle().set("margin", "0 auto");
    setPadding(true);
    setSpacing(true);

    add(new H2("Nutzerverwaltung"));

    grid.addColumn(RegisteredUser::getDisplayName).setHeader("Name").setSortable(true);
    grid.addColumn(RegisteredUser::getLoginName).setHeader("Username").setSortable(true);
    grid.addColumn(user -> user.getStatus().name()).setHeader("Status").setSortable(true);
    grid.addColumn(user -> user.getRole().name()).setHeader("Rolle").setSortable(true);

    grid.addComponentColumn(
            user -> {
              Button manageBtn = new Button("Verwalten", e -> openManageDialog(user));
              manageBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
              return manageBtn;
            })
        .setHeader("Aktionen");

    add(grid);
    refreshGrid();
  }

  private void refreshGrid() {
    grid.setItems(adminWorkflows.listAllUsers());
  }

  private void openManageDialog(RegisteredUser user) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Nutzer verwalten: " + user.getDisplayName());

    VerticalLayout layout = new VerticalLayout();

    // Details Section
    ZonedDateTime createdDate = user.getTsCreation().atZone(ZoneId.systemDefault());
    layout.add(
        new Paragraph(
            "Nutzer erstellt: "
                + (user.getTsLastLogin() != null
                    ? localeService.getDateTimeFormatter().format(createdDate)
                    : "Nie")));
    ZonedDateTime lastLogin = user.getTsLastLogin().atZone(ZoneId.systemDefault());
    layout.add(
        new Paragraph(
            "Zuletzt eingeloggt: "
                + (user.getTsLastLogin() != null
                    ? localeService.getDateTimeFormatter().format(lastLogin)
                    : "Nie")));

    String intro = adminWorkflows.getRegistrationIntroText(user.getId());
    Paragraph introPara = new Paragraph(intro);
    introPara
        .getStyle()
        .set("white-space", "pre-wrap")
        .set("font-size", "var(--lumo-font-size-s)")
        .set("background", "var(--lumo-contrast-5pct)")
        .set("padding", "var(--lumo-space-s)");
    layout.add(new H3("Bewerbungstext"), introPara);

    // Actions Section
    HorizontalLayout actions = new HorizontalLayout();

    Button toggleBanBtn =
        new Button(user.getStatus() == UserStatus.BANNED ? "Entsperren" : "Sperren");
    toggleBanBtn.addThemeVariants(
        user.getStatus() == UserStatus.BANNED
            ? ButtonVariant.LUMO_SUCCESS
            : ButtonVariant.LUMO_ERROR);
    toggleBanBtn.addClickListener(
        e -> {
          adminWorkflows.toggleUserBanStatus(user.getId());
          refreshGrid();
          dialog.close();
          Notification.show("Status aktualisiert")
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    Button tempPwBtn = new Button("Temporäres Passwort generieren");
    tempPwBtn.addClickListener(
        e -> {
          String tempPw = adminWorkflows.generateAndSetTemporaryPassword(user.getId());
          Notification success =
              Notification.show(
                  "Passwort für " + user.getLoginName() + ": " + tempPw,
                  10000,
                  Notification.Position.MIDDLE);
          success.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
        });

    actions.add(toggleBanBtn, tempPwBtn);
    layout.add(new H3("Aktionen"), actions);

    dialog.add(layout);

    Button closeBtn = new Button("Schließen", e -> dialog.close());
    dialog.getFooter().add(closeBtn);

    dialog.open();
  }
}

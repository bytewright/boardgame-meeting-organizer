package org.bytewright.bgmo.adapter.api.frontend.view.admin;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.time.Instant;
import java.time.ZonedDateTime;
import org.bytewright.bgmo.adapter.api.frontend.service.i18n.LocaleService;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.model.user.UserRole;
import org.bytewright.bgmo.domain.model.user.UserStatus;
import org.bytewright.bgmo.domain.service.SiteManagementService;
import org.bytewright.bgmo.usecases.AdminWorkflows;

@Route(value = "admin/users", layout = MainLayout.class)
@PageTitle("User management | " + APP_NAME_SHORT)
@RolesAllowed("ADMIN")
public class AdminUserManagementView extends VerticalLayout {
  private final LocaleService localeService;
  private final AdminWorkflows adminWorkflows;
  private final SiteManagementService siteManagementService;
  private final Grid<RegisteredUser> grid = new Grid<>(RegisteredUser.class, false);

  public AdminUserManagementView(
      LocaleService localeService,
      AdminWorkflows adminWorkflows,
      SiteManagementService siteManagementService) {
    this.localeService = localeService;
    this.adminWorkflows = adminWorkflows;
    this.siteManagementService = siteManagementService;

    setMaxWidth(MainLayout.MAX_DISPLAYPORT_WIDTH);
    getStyle().set("margin", "0 auto");
    setPadding(true);
    setSpacing(true);

    add(new H2("Nutzerverwaltung"));

    // ── Grid columns ──────────────────────────────────────────────────────────
    grid.addColumn(RegisteredUser::getDisplayName)
        .setHeader("Name")
        .setSortable(true)
        .setAutoWidth(true);

    grid.addColumn(RegisteredUser::getLoginName)
        .setHeader("Username")
        .setSortable(true)
        .setAutoWidth(true);

    grid.addColumn(user -> user.getStatus().name())
        .setHeader("Status")
        .setSortable(true)
        .setAutoWidth(true);

    grid.addColumn(user -> user.getRole().name())
        .setHeader("Rolle")
        .setSortable(true)
        .setAutoWidth(true);

    grid.addColumn(user -> formatInstant(user.getTsCreation()))
        .setHeader("Erstellt")
        .setSortable(true)
        .setAutoWidth(true)
        .setComparator(RegisteredUser::getTsCreation);

    grid.addColumn(user -> formatInstant(user.getTsModified()))
        .setHeader("Geändert")
        .setSortable(true)
        .setAutoWidth(true)
        .setComparator(RegisteredUser::getTsModified);

    grid.addColumn(user -> formatInstant(user.getTsLastLogin()))
        .setHeader("Letzter Login")
        .setSortable(true)
        .setAutoWidth(true)
        .setComparator(RegisteredUser::getTsLastLogin);

    grid.addComponentColumn(
            user -> {
              Button manageBtn = new Button("Verwalten", e -> openManageDialog(user));
              manageBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
              return manageBtn;
            })
        .setHeader("Aktionen")
        .setAutoWidth(true);

    add(grid);
    refreshGrid();
  }

  private String formatInstant(Instant instant) {
    if (instant == null) {
      return "—";
    }
    ZonedDateTime zdt = instant.atZone(siteManagementService.getServiceTimeZone());
    return localeService.getDateTimeFormatter().format(zdt);
  }

  private void refreshGrid() {
    grid.setItems(adminWorkflows.listAllUsers());
  }

  private void openManageDialog(RegisteredUser user) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Nutzer verwalten: " + user.getDisplayName());
    dialog.setWidth("600px");

    VerticalLayout layout = new VerticalLayout();
    layout.setPadding(false);

    // ── Status ────────────────────────────────────────────────────────────
    layout.add(new H3("Status & Rolle"));

    Select<UserStatus> statusSelect = new Select<>();
    statusSelect.setLabel("Status");
    statusSelect.setItems(UserStatus.values());
    statusSelect.setItemLabelGenerator(UserStatus::name);
    statusSelect.setValue(user.getStatus());

    Select<UserRole> roleSelect = new Select<>();
    roleSelect.setLabel("Rolle");
    roleSelect.setItems(UserRole.values());
    roleSelect.setItemLabelGenerator(UserRole::name);
    roleSelect.setValue(user.getRole());

    Button saveStatusBtn = new Button("Status speichern");
    saveStatusBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveStatusBtn.addClickListener(
        e -> {
          UserStatus selectedStatus = statusSelect.getValue();
          adminWorkflows.setUserStatus(user.getId(), selectedStatus);
          refreshGrid();
          Notification.show("Änderungen gespeichert.")
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
    Button saveRoleBtn = new Button("Rolle speichern");
    saveRoleBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveRoleBtn.addClickListener(
        e -> {
          UserRole selectedRole = roleSelect.getValue();
          adminWorkflows.setUserRole(user.getId(), selectedRole);
          refreshGrid();
          Notification.show("Änderungen gespeichert.")
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    HorizontalLayout statusUpdateRow = new HorizontalLayout(statusSelect, saveStatusBtn);

    statusUpdateRow.setAlignItems(Alignment.END);
    statusUpdateRow.setWidthFull();
    HorizontalLayout roleUpdateRow = new HorizontalLayout(roleSelect, saveRoleBtn);
    roleUpdateRow.setAlignItems(Alignment.END);
    roleUpdateRow.setWidthFull();
    layout.add(statusUpdateRow, roleUpdateRow);

    // ── Quick actions ─────────────────────────────────────────────────────
    layout.add(new H3("Schnellaktionen"));
    HorizontalLayout quickActions = new HorizontalLayout();

    Button suspendBtn = new Button("Sperren (SUSPENDED)");
    suspendBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
    suspendBtn.setEnabled(user.getStatus() != UserStatus.SUSPENDED);
    suspendBtn.addClickListener(
        e -> {
          adminWorkflows.setUserStatus(user.getId(), UserStatus.SUSPENDED);
          refreshGrid();
          dialog.close();
          Notification.show("Nutzer gesperrt.").addThemeVariants(NotificationVariant.LUMO_WARNING);
        });

    Button tempPwBtn = new Button("Temporäres Passwort");
    tempPwBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
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

    quickActions.add(suspendBtn, tempPwBtn);
    layout.add(quickActions);

    dialog.add(layout);

    Button closeBtn = new Button("Schließen", e -> dialog.close());
    dialog.getFooter().add(closeBtn);

    dialog.open();
  }
}

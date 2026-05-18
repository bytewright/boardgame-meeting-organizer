package org.bytewright.bgmo.adapter.api.frontend.view.admin;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bytewright.bgmo.adapter.api.frontend.service.SessionInfoService;
import org.bytewright.bgmo.adapter.api.frontend.view.LoginView;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.usecases.AdminWorkflows;

@Route(value = "admin/broadcast", layout = MainLayout.class)
@PageTitle("Nachrichten | " + APP_NAME_SHORT)
@RolesAllowed("ADMIN")
public class AdminBroadcastView extends VerticalLayout implements BeforeEnterObserver {

  private static final String MODE_BROADCAST = "Broadcast (alle aktiven Nutzer)";
  private static final String MODE_DIRECT = "Direktnachricht";

  private final SessionInfoService sessionInfoService;
  private final AdminWorkflows adminWorkflows;

  public AdminBroadcastView(SessionInfoService sessionInfoService, AdminWorkflows adminWorkflows) {
    this.sessionInfoService = sessionInfoService;
    this.adminWorkflows = adminWorkflows;

    setMaxWidth(MainLayout.MAX_DISPLAYPORT_WIDTH);
    getStyle().set("margin", "0 auto");
    setPadding(true);
    setSpacing(true);

    add(new H2("Nachrichten senden"));

    // ── Mode selector ────────────────────────────────────────────────────────
    RadioButtonGroup<String> modeGroup = new RadioButtonGroup<>();
    modeGroup.setLabel("Modus");
    modeGroup.setItems(MODE_BROADCAST, MODE_DIRECT);
    modeGroup.setValue(MODE_BROADCAST);

    // ── Recipient selector (only shown for direct messages) ──────────────────
    Select<RegisteredUser> userSelect = new Select<>();
    userSelect.setLabel("Empfänger");
    userSelect.setItemLabelGenerator(
        user -> user.getDisplayName() + " (" + user.getLoginName() + ")");
    List<RegisteredUser> allUsers = adminWorkflows.listAllUsers();
    userSelect.setItems(allUsers);
    userSelect.setWidthFull();
    userSelect.setVisible(false);

    modeGroup.addValueChangeListener(e -> userSelect.setVisible(MODE_DIRECT.equals(e.getValue())));

    // ── Message input ────────────────────────────────────────────────────────
    TextArea messageArea = new TextArea("Nachricht");
    messageArea.setWidthFull();
    messageArea.setMinHeight("150px");
    messageArea.setPlaceholder("Nachricht eingeben …");

    // ── Hint text ────────────────────────────────────────────────────────────
    Paragraph hint =
        new Paragraph(
            "Broadcasts werden nur an Nutzer mit Status ACTIVE gesendet. "
                + "Direktnachrichten gehen unabhängig vom Status raus.");
    hint.getStyle()
        .set("font-size", "var(--lumo-font-size-s)")
        .set("color", "var(--lumo-secondary-text-color)");

    // ── Send button ──────────────────────────────────────────────────────────
    Button sendBtn = new Button("Senden");
    sendBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    sendBtn.addClickListener(e -> handleSend(modeGroup, userSelect, messageArea));

    HorizontalLayout controls = new HorizontalLayout(modeGroup, userSelect);
    controls.setWidthFull();
    controls.setAlignItems(Alignment.END);

    add(new H3("Neue Nachricht"), controls, messageArea, hint, sendBtn);
  }

  private void handleSend(
      RadioButtonGroup<String> modeGroup, Select<RegisteredUser> userSelect, TextArea messageArea) {

    String message = messageArea.getValue();
    if (message == null || message.isBlank()) {
      Notification.show("Nachricht darf nicht leer sein.")
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
      return;
    }

    Optional<RegisteredUser> adminOpt = sessionInfoService.getCurrentUser();
    if (adminOpt.isEmpty()) {
      UI.getCurrent().navigate(LoginView.class);
      return;
    }
    UUID adminId = adminOpt.get().getId();

    if (MODE_BROADCAST.equals(modeGroup.getValue())) {
      adminWorkflows.broadcastToAllUsers(adminId, message);
      Notification.show("Broadcast erfolgreich gesendet!")
          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
      messageArea.clear();
    } else {
      RegisteredUser target = userSelect.getValue();
      if (target == null) {
        Notification.show("Bitte einen Empfänger auswählen.")
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
        return;
      }
      adminWorkflows.dispatchToUser(adminId, target.getId(), message);
      Notification.show("Nachricht an " + target.getDisplayName() + " gesendet!")
          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
      messageArea.clear();
      userSelect.clear();
    }
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (sessionInfoService.getCurrentUser().isEmpty()) {
      event.forwardTo(LoginView.class);
    }
  }
}

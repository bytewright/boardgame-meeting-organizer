package org.bytewright.bgmo.adapter.api.frontend.view.admin;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.Comparator;
import java.util.List;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.domain.model.AdapterSettings;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.service.SiteOperatorInfoService;
import org.bytewright.bgmo.domain.service.data.AdapterSettingsDao;
import org.bytewright.bgmo.usecases.AdminWorkflows;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Route(value = "admin/site-settings", layout = MainLayout.class)
@PageTitle("Site-Einstellungen | " + APP_NAME_SHORT)
@RolesAllowed("ADMIN")
public class AdminSiteSettingsView extends VerticalLayout {

  private final JsonMapper objectMapper;

  public AdminSiteSettingsView(
      SiteOperatorInfoService operatorInfoService,
      AdapterSettingsDao adapterSettingsDao,
      AdminWorkflows adminWorkflows,
      JsonMapper objectMapper) {
    this.objectMapper = objectMapper;

    setMaxWidth(MainLayout.MAX_DISPLAYPORT_WIDTH);
    getStyle().set("margin", "0 auto");
    setPadding(true);
    setSpacing(true);

    add(new H2("Site-Einstellungen"));

    buildOperatorInfoSection(operatorInfoService);
    buildAdapterSettingsSection(adapterSettingsDao, adminWorkflows);
  }

  // ── Operator info (read-only display) ────────────────────────────────────────

  private void buildOperatorInfoSection(SiteOperatorInfoService operatorInfoService) {
    add(sectionHeading("Betreiberdaten (Impressum)"));
    add(
        new Paragraph(
            "Diese Daten werden im Impressum und in der Datenschutzerklärung verwendet. "
                + "Änderungen erfolgen direkt in der Datenbank oder über die SiteOperatorInfoService-Implementierung."));

    ContactInfo.AddressContact address = operatorInfoService.getOperatorAddress();

    VerticalLayout info = new VerticalLayout();
    info.setPadding(true);
    info.setSpacing(false);
    info.getStyle()
        .set("background", "var(--lumo-contrast-5pct)")
        .set("border-radius", "var(--lumo-border-radius-m)");

    info.add(infoRow("Name", address.nameOnBell()));
    info.add(infoRow("Straße", address.street()));
    info.add(infoRow("PLZ / Ort", address.zipCode() + " " + address.city()));

    operatorInfoService
        .getOperatorPhone()
        .ifPresentOrElse(
            phone -> info.add(infoRow("Telefon", phone.phoneNr())),
            () -> info.add(infoRow("Telefon", "— nicht hinterlegt")));

    operatorInfoService
        .getOperatorEmail()
        .ifPresentOrElse(
            email -> info.add(infoRow("E-Mail", email.email())),
            () -> info.add(infoRow("E-Mail", "— nicht hinterlegt (gesetzlich erforderlich!)")));

    add(info);
  }

  private HorizontalLayout infoRow(String label, String value) {
    Span labelSpan = new Span(label + ":");
    labelSpan
        .getStyle()
        .set("font-weight", "bold")
        .set("min-width", "120px")
        .set("color", "var(--lumo-secondary-text-color)");
    Span valueSpan = new Span(value);
    HorizontalLayout row = new HorizontalLayout(labelSpan, valueSpan);
    row.setSpacing(true);
    row.setPadding(false);
    return row;
  }

  // ── Adapter settings (JSON view/edit) ────────────────────────────────────────

  private void buildAdapterSettingsSection(
      AdapterSettingsDao adapterSettingsDao, AdminWorkflows adminWorkflows) {
    add(sectionHeading("Adapter-Einstellungen"));
    add(
        new Paragraph(
            "Jeder Adapter speichert seine Konfiguration als JSON. "
                + "Der JSON-Inhalt wird nicht validiert; ungültige Werte können den jeweiligen Adapter beeinträchtigen."));

    List<AdapterSettings> allSettings =
        adapterSettingsDao.findAll().stream()
            .sorted(Comparator.comparing(AdapterSettings::getAdapterName))
            .toList();

    if (allSettings.isEmpty()) {
      add(new Span("Keine Adapter-Einstellungen vorhanden."));
      return;
    }

    for (AdapterSettings settings : allSettings) {
      add(buildAdapterCard(settings, adminWorkflows));
    }
  }

  private VerticalLayout buildAdapterCard(AdapterSettings settings, AdminWorkflows adminWorkflows) {
    H3 adapterName = new H3(settings.getAdapterName());
    adapterName.getStyle().set("margin", "0 0 var(--lumo-space-s) 0");

    TextArea jsonArea = new TextArea();
    jsonArea.setWidthFull();
    jsonArea.setMinHeight("150px");
    jsonArea.setValue(prettyPrint(settings.getAdapterSettings()));
    jsonArea.getStyle().set("font-family", "monospace").set("font-size", "var(--lumo-font-size-s)");

    Span validationMsg = new Span();
    validationMsg.getStyle().set("font-size", "var(--lumo-font-size-s)");

    // Live JSON validation on change
    jsonArea.addValueChangeListener(
        e -> {
          boolean valid = isValidJson(e.getValue());
          validationMsg.setText(valid ? "✓ Gültiges JSON" : "✗ Ungültiges JSON");
          validationMsg
              .getStyle()
              .set("color", valid ? "var(--lumo-success-color)" : "var(--lumo-error-color)");
        });

    Button saveBtn =
        new Button(
            "Speichern",
            VaadinIcon.CHECK.create(),
            e -> {
              String newJson = jsonArea.getValue().trim();
              if (!isValidJson(newJson)) {
                showError("Speichern fehlgeschlagen: Ungültiges JSON.");
                return;
              }
              adminWorkflows.updateAdapterSettings(settings, newJson);
              showSuccess("Einstellungen für \"" + settings.getAdapterName() + "\" gespeichert.");
            });
    saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

    Button resetBtn =
        new Button(
            "Zurücksetzen",
            VaadinIcon.REFRESH.create(),
            e -> {
              jsonArea.setValue(prettyPrint(settings.getAdapterSettings()));
              validationMsg.setText("");
            });
    resetBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

    HorizontalLayout actions = new HorizontalLayout(saveBtn, resetBtn, validationMsg);
    actions.setAlignItems(Alignment.CENTER);

    VerticalLayout card = new VerticalLayout(adapterName, jsonArea, actions);
    card.setPadding(true);
    card.setSpacing(false);
    card.getStyle()
        .set("border", "1px solid var(--lumo-contrast-20pct)")
        .set("border-radius", "var(--lumo-border-radius-l)")
        .set("margin-bottom", "var(--lumo-space-m)");

    return card;
  }

  // ── Helpers ──────────────────────────────────────────────────────────────────

  private H2 sectionHeading(String title) {
    H2 h = new H2(title);
    h.getStyle()
        .set("font-size", "var(--lumo-font-size-l)")
        .set("margin-top", "var(--lumo-space-xl)")
        .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
        .set("padding-bottom", "var(--lumo-space-xs)");
    return h;
  }

  private String prettyPrint(String json) {
    try {
      Object parsed = objectMapper.readValue(json, Object.class);
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
    } catch (JacksonException e) {
      return json; // return raw if already malformed
    }
  }

  private boolean isValidJson(String json) {
    try {
      objectMapper.readTree(json);
      return true;
    } catch (JacksonException e) {
      return false;
    }
  }

  private void showSuccess(String message) {
    Notification n = Notification.show(message, 3000, Notification.Position.BOTTOM_CENTER);
    n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }

  private void showError(String message) {
    Notification n = Notification.show(message, 4000, Notification.Position.BOTTOM_CENTER);
    n.addThemeVariants(NotificationVariant.LUMO_ERROR);
  }
}

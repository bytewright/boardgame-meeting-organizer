package org.bytewright.bgmo.adapter.api.frontend.view;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.service.SiteOperatorInfoService;

@Route(value = "datenschutz", layout = MainLayout.class)
@PageTitle("Datenschutzerklärung | " + APP_NAME_SHORT)
@AnonymousAllowed
public class DatenschutzView extends VerticalLayout {

  public DatenschutzView(SiteOperatorInfoService operatorInfoService) {
    setMaxWidth(MainLayout.MAX_DISPLAYPORT_WIDTH);
    getStyle().set("margin", "0 auto");
    setPadding(true);
    setSpacing(true);

    ContactInfo.AddressContact address = operatorInfoService.getOperatorAddress();
    String operatorName = address.nameOnBell();
    String operatorAddress = address.street() + ", " + address.zipCode() + " " + address.city();
    String operatorEmail =
        operatorInfoService
            .getOperatorEmail()
            .map(ContactInfo.EmailContact::email)
            .orElse("(keine E-Mail hinterlegt)");

    boolean telegramActive = operatorInfoService.isTelegramIntegrationActive();
    boolean signalActive = operatorInfoService.isSignalIntegrationActive();

    add(new H1("Datenschutzerklärung"));

    // ── 1. Controller ────────────────────────────────────────────────────────
    add(section("1. Verantwortlicher"));
    add(
        new Paragraph(
            "Verantwortlicher im Sinne der DSGVO ist: "
                + operatorName
                + ", "
                + operatorAddress
                + ". "
                + "Kontakt: "
                + operatorEmail));

    // ── 2. Hosting / Server ──────────────────────────────────────────────────
    add(section("2. Hosting und technischer Betrieb"));
    add(
        new Paragraph(
            "Die Anwendung wird auf einem privaten Server betrieben. "
                + "Beim Zugriff auf die Seite werden serverseitig Verbindungsdaten "
                + "(IP-Adresse, Zeitstempel, aufgerufene URL) temporär in Logdateien gespeichert. "
                + "Diese Daten werden ausschließlich zur Fehlerdiagnose verwendet und nach 30 Tagen gelöscht. "
                + "Rechtsgrundlage: Art. 6 Abs. 1 lit. f DSGVO (berechtigtes Interesse am sicheren Betrieb)."));

    // ── 3. Account data ──────────────────────────────────────────────────────
    add(section("3. Nutzerkonto und Anmeldedaten"));
    add(
        new Paragraph(
            "Zur Nutzung der Anwendung ist eine Registrierung erforderlich. "
                + "Dabei werden Benutzername, Anzeigename und ein Passwort-Hash gespeichert. "
                + "Neu registrierte Konten werden erst nach manueller Freischaltung durch den Administrator aktiviert. "
                + "Die Daten werden für die Dauer der Mitgliedschaft gespeichert. "
                + "Rechtsgrundlage: Art. 6 Abs. 1 lit. b DSGVO (Vertragserfüllung)."));

    // ── 4. Contact info ──────────────────────────────────────────────────────
    add(section("4. Freiwillige Kontaktangaben"));
    add(
        new Paragraph(
            "Nutzer können freiwillig Kontaktdaten hinterlegen (E-Mail-Adresse, Postanschrift), "
                + "um über Veranstaltungen informiert zu werden oder als Veranstaltungsort gelistet zu sein. "
                + "Diese Angaben sind optional und können jederzeit gelöscht werden. "
                + "Rechtsgrundlage: Art. 6 Abs. 1 lit. a DSGVO (Einwilligung)."));

    // ── 5. Cookies / Session ─────────────────────────────────────────────────
    add(section("5. Cookies und Session"));
    add(
        new Paragraph(
            "Die Anwendung verwendet ausschließlich technisch notwendige Session-Cookies, "
                + "die für den Betrieb der Weboberfläche erforderlich sind. "
                + "Es werden keine Tracking- oder Marketing-Cookies eingesetzt. "
                + "Eine Einwilligung ist gemäß § 25 Abs. 2 TTDSG nicht erforderlich."));

    // ── 6. Telegram (conditional) ────────────────────────────────────────────
    if (telegramActive) {
      add(section("6. Telegram-Bot-Integration"));
      add(
          new Paragraph(
              "Die Anwendung bietet eine optionale Integration mit dem Messenger-Dienst Telegram "
                  + "(Telegram Messenger Inc., 548 Market St #15819, San Francisco, CA 94104, USA). "
                  + "Wenn Sie Ihre Telegram-ID hinterlegen, kann der Bot Sie über neue Meetups benachrichtigen. "
                  + "Hierbei werden Ihre Nachrichten über die Server von Telegram übertragen. "
                  + "Datenschutzerklärung von Telegram: https://telegram.org/privacy. "
                  + "Rechtsgrundlage: Art. 6 Abs. 1 lit. a DSGVO (Einwilligung). "
                  + "Die Einwilligung kann jederzeit durch Entfernen der Telegram-ID widerrufen werden."));
    }

    // ── 7. Signal (conditional) ──────────────────────────────────────────────
    if (signalActive) {
      int sectionNr = telegramActive ? 7 : 6;
      add(section(sectionNr + ". Signal-Integration"));
      add(
          new Paragraph(
              "Die Anwendung bietet eine optionale Integration mit Signal (Signal Messenger LLC). "
                  + "Wenn Sie Ihre Signal-Kontaktkennung hinterlegen, können Sie über Meetups benachrichtigt werden. "
                  + "Datenschutzerklärung von Signal: https://signal.org/legal/. "
                  + "Rechtsgrundlage: Art. 6 Abs. 1 lit. a DSGVO (Einwilligung). "
                  + "Die Einwilligung kann jederzeit durch Entfernen der Telegram-ID widerrufen werden."));
    }

    // ── Rights ───────────────────────────────────────────────────────────────
    int lastSection = 6 + (telegramActive ? 1 : 0) + (signalActive ? 1 : 0);
    add(section((lastSection) + ". Ihre Rechte"));
    add(
        new Paragraph(
            "Sie haben das Recht auf Auskunft (Art. 15 DSGVO), Berichtigung (Art. 16 DSGVO), "
                + "Löschung (Art. 17 DSGVO), Einschränkung der Verarbeitung (Art. 18 DSGVO) "
                + "sowie Datenübertragbarkeit (Art. 20 DSGVO). "
                + "Zur Ausübung Ihrer Rechte wenden Sie sich bitte an: "
                + operatorEmail
                + ". "
                + "Sie haben zudem das Recht, Beschwerde bei der zuständigen Datenschutz-Aufsichtsbehörde einzulegen."));
  }

  private H2 section(String title) {
    H2 h = new H2(title);
    h.getStyle()
        .set("font-size", "var(--lumo-font-size-l)")
        .set("margin-top", "var(--lumo-space-l)");
    return h;
  }
}

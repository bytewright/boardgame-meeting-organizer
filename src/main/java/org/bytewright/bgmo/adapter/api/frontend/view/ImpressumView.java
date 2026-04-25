package org.bytewright.bgmo.adapter.api.frontend.view;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.service.SiteOperatorInfoService;

@Route(value = "impressum", layout = MainLayout.class)
@PageTitle("Impressum | " + APP_NAME_SHORT)
@AnonymousAllowed
public class ImpressumView extends VerticalLayout {

  public ImpressumView(SiteOperatorInfoService operatorInfoService) {
    setMaxWidth(MainLayout.MAX_DISPLAYPORT_WIDTH);
    getStyle().set("margin", "0 auto");
    setPadding(true);
    setSpacing(true);

    add(new H1("Impressum"));
    add(section("Angaben gemäß § 5 TMG"));

    ContactInfo.AddressContact address = operatorInfoService.getOperatorAddress();
    add(lines(address.nameOnBell(), address.street(), address.zipCode() + " " + address.city()));

    add(section("Kontakt"));
    operatorInfoService
        .getOperatorPhone()
        .ifPresent(phone -> add(labeledLine("Telefon:", phone.phoneNr())));
    operatorInfoService
        .getOperatorEmail()
        .ifPresent(email -> add(labeledLine("E-Mail:", email.email())));

    add(section("Hinweis zur Streitschlichtung"));
    add(
        new Paragraph(
            "Die Europäische Kommission stellt eine Plattform zur Online-Streitbeilegung (OS) bereit: "
                + "https://ec.europa.eu/consumers/odr/. Wir sind nicht verpflichtet und nicht bereit, "
                + "an Streitbeilegungsverfahren vor einer Verbraucherschlichtungsstelle teilzunehmen, "
                + "da es sich um ein privates, nicht-kommerzielles Angebot handelt."));
  }

  private H2 section(String title) {
    H2 h = new H2(title);
    h.getStyle()
        .set("font-size", "var(--lumo-font-size-l)")
        .set("margin-top", "var(--lumo-space-l)");
    return h;
  }

  /** Renders a block of plain text lines (like an address block). */
  private VerticalLayout lines(String... lines) {
    VerticalLayout block = new VerticalLayout();
    block.setPadding(false);
    block.setSpacing(false);
    for (String line : lines) {
      block.add(new Span(line));
    }
    return block;
  }

  private Span labeledLine(String label, String value) {
    Span s = new Span(label + " " + value);
    return s;
  }
}

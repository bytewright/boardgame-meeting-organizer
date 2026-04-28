package org.bytewright.bgmo.adapter.api.frontend.view.legal;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.domain.service.SiteOperatorInfoService;

@Route(value = "tos", layout = MainLayout.class)
@PageTitle("Nutzungsbedingungen | " + APP_NAME_SHORT)
@AnonymousAllowed
public class TermsOfUseView extends VerticalLayout {

  public TermsOfUseView(SiteOperatorInfoService operatorInfoService) {
    setMaxWidth(MainLayout.MAX_DISPLAYPORT_WIDTH);
    getStyle().set("margin", "0 auto");
    setPadding(true);
    setSpacing(true);

    add(new H1("Nutzungsbedingungen"));
    for (String tosSection : operatorInfoService.getTosText()) {
      add(tosParagraphSection(tosSection));
    }
  }

  private Paragraph tosParagraphSection(String tosText) {
    return new Paragraph(tosText);
  }
}

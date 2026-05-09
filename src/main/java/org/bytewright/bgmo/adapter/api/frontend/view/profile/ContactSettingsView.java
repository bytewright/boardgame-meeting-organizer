package org.bytewright.bgmo.adapter.api.frontend.view.profile;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.bytewright.bgmo.adapter.api.frontend.SessionAuthenticationService;
import org.bytewright.bgmo.adapter.api.frontend.view.LoginView;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.adapter.api.frontend.view.component.factory.ComponentFactory;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;

@Route(value = "profile/contacts", layout = MainLayout.class)
@PageTitle("Contact Info | " + APP_NAME_SHORT)
@PermitAll
@RequiredArgsConstructor
public class ContactSettingsView extends VerticalLayout implements BeforeEnterObserver {

  private final SessionAuthenticationService authService;
  private final ComponentFactory componentFactory;

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Optional<RegisteredUser> userOpt = authService.getCurrentUser();
    if (userOpt.isEmpty()) {
      event.forwardTo(LoginView.class);
      return;
    }
    buildView(userOpt.get());
  }

  private void buildView(RegisteredUser user) {
    removeAll();
    setAlignItems(Alignment.CENTER);
    setWidthFull();
    setPadding(true);

    H2 title = new H2(getTranslation("profile.contacts.title"));
    title.getStyle().set("max-width", MainLayout.MAX_DISPLAYPORT_WIDTH).set("width", "100%");

    ContactSection contactSection = componentFactory.contactSection(user);
    contactSection
        .getStyle()
        .set("max-width", MainLayout.MAX_DISPLAYPORT_WIDTH)
        .set("width", "100%");

    add(title, contactSection);
  }
}

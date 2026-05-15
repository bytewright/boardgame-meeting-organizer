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
import org.bytewright.bgmo.adapter.api.frontend.service.SessionInfoService;
import org.bytewright.bgmo.adapter.api.frontend.view.LoginView;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.adapter.api.frontend.view.component.factory.ComponentFactory;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;

@Route(value = "profile/library", layout = MainLayout.class)
@PageTitle("Game Library | " + APP_NAME_SHORT)
@PermitAll
@RequiredArgsConstructor
public class GameLibView extends VerticalLayout implements BeforeEnterObserver {

  private final SessionInfoService authService;
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

    setMaxWidth(MainLayout.MAX_DISPLAYPORT_WIDTH);
    getStyle().setMargin("0 auto");

    H2 title = new H2(getTranslation("profile.library.title"));

    GameLibSection gameLibSection = componentFactory.gameLibSection(user);
    gameLibSection.setMaxWidth(MainLayout.MAX_DISPLAYPORT_WIDTH);
    gameLibSection.setWidthFull();

    add(title, gameLibSection);
  }
}

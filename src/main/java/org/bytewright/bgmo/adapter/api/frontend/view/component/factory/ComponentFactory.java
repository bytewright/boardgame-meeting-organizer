package org.bytewright.bgmo.adapter.api.frontend.view.component.factory;

import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.bytewright.bgmo.adapter.api.frontend.service.i18n.LocaleService;
import org.bytewright.bgmo.adapter.api.frontend.view.component.AddGameDialog;
import org.bytewright.bgmo.adapter.api.frontend.view.component.ContactSection;
import org.bytewright.bgmo.adapter.api.frontend.view.component.GameLibSection;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.GameInformationProvider;
import org.bytewright.bgmo.domain.service.data.GameDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.domain.service.notification.VerificationCodeService;
import org.bytewright.bgmo.usecases.UserWorkflows;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ComponentFactory {
  private final Set<GameInformationProvider> providerList;
  private final VerificationCodeService verificationService;
  private final LocaleService localeService;
  private final UserWorkflows userWorkflows;
  private final RegisteredUserDao userDao;
  private final GameDao gameDao;

  public ContactSection contactSection(RegisteredUser currentUser) {
    Map<ContactInfoType, String> botHandles = verificationService.getBotHandles();
    return new ContactSection(
        this, verificationService, userWorkflows, userDao, currentUser, botHandles);
  }

  public GameLibSection gameLibSection(RegisteredUser currentUser) {
    return new GameLibSection(this, providerList, userWorkflows, gameDao, currentUser);
  }

  public AddGameDialog addGameDialog() {
    return new AddGameDialog();
  }
}

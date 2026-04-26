package org.bytewright.bgmo.adapter.api.frontend.view.component.factory;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bytewright.bgmo.adapter.api.frontend.view.component.ContactSection;
import org.bytewright.bgmo.adapter.api.frontend.view.component.GameLibSection;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.GameDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.domain.service.notification.VerificationCodeService;
import org.bytewright.bgmo.usecases.UserWorkflows;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ComponentFactory {
  private final VerificationCodeService verificationService;
  private final UserWorkflows userWorkflows;
  private final RegisteredUserDao userDao;
  private final GameDao gameDao;

  public ContactSection contactSection(RegisteredUser currentUser) {
    Map<ContactInfoType, String> botHandles = verificationService.getBotHandles();
    return new ContactSection(verificationService, userWorkflows, userDao, currentUser, botHandles);
  }

  public GameLibSection gameLibSection(RegisteredUser currentUser) {
    return new GameLibSection(userWorkflows, gameDao, currentUser);
  }
}

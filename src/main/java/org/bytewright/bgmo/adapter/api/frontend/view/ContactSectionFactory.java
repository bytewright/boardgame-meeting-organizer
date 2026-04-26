package org.bytewright.bgmo.adapter.api.frontend.view;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bytewright.bgmo.adapter.api.frontend.view.component.ContactSection;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.domain.service.notification.VerificationCodeService;
import org.bytewright.bgmo.usecases.UserWorkflows;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContactSectionFactory {
  private final VerificationCodeService verificationService;
  private final UserWorkflows userWorkflows;
  private final RegisteredUserDao userDao;

  public ContactSection contactSection(RegisteredUser currentUser) {
    Map<ContactInfoType, String> botHandles = verificationService.getBotHandles();
    return new ContactSection(verificationService, userWorkflows, userDao, currentUser, botHandles);
  }
}

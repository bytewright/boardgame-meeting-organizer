package org.bytewright.bgmo.adapter.api.frontend.view.component.factory;

import com.vaadin.flow.server.VaadinSession;
import java.util.*;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import org.bytewright.bgmo.adapter.api.frontend.service.i18n.LocaleService;
import org.bytewright.bgmo.adapter.api.frontend.view.component.AddGameDialog;
import org.bytewright.bgmo.adapter.api.frontend.view.component.ContactSection;
import org.bytewright.bgmo.adapter.api.frontend.view.component.GameLibSection;
import org.bytewright.bgmo.domain.model.Game;
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
    return new GameLibSection(this, userWorkflows, gameDao, currentUser);
  }

  public AddGameDialog addGameDialog(RegisteredUser currentUser, Runnable runnable) {
    Locale userLocale = VaadinSession.getCurrent().getLocale();
    List<GameInformationProvider> list =
        providerList.stream()
            .sorted(
                Comparator.comparing(
                    gip -> gip.getInputConfig(userLocale).getProviderDisplayName()))
            .toList();
    BiConsumer<RegisteredUser, Game.Creation> saveConsumer =
        (registeredUser, creation) -> {
          userWorkflows.addGameToLibrary(registeredUser.getId(), creation);
          runnable.run();
        };
    AddGameDialog addGameDialog = new AddGameDialog(list, currentUser, saveConsumer);
    return addGameDialog;
  }
}

package org.bytewright.bgmo.adapter.api.frontend.view.component.factory;

import com.vaadin.flow.server.VaadinSession;
import java.util.*;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import org.bytewright.bgmo.adapter.api.frontend.service.SessionInfoService;
import org.bytewright.bgmo.adapter.api.frontend.service.i18n.LocaleService;
import org.bytewright.bgmo.adapter.api.frontend.view.component.LocalePicker;
import org.bytewright.bgmo.adapter.api.frontend.view.meetup.component.AttendeeRequestCard;
import org.bytewright.bgmo.adapter.api.frontend.view.meetup.model.MeetupAttendeesContext;
import org.bytewright.bgmo.adapter.api.frontend.view.profile.*;
import org.bytewright.bgmo.domain.model.Game;
import org.bytewright.bgmo.domain.model.notification.MessengerLinkContext;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.GameInformationProvider;
import org.bytewright.bgmo.domain.service.data.GameDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.domain.service.notification.NotificationLinkCodeService;
import org.bytewright.bgmo.domain.service.user.ContactInfoService;
import org.bytewright.bgmo.usecases.AdminWorkflows;
import org.bytewright.bgmo.usecases.MeetupWorkflows;
import org.bytewright.bgmo.usecases.UserWorkflows;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ComponentFactory {
  private final Set<GameInformationProvider> providerList;
  private final ContactInfoService contactInfoService;
  private final NotificationLinkCodeService verificationService;
  private final SessionInfoService authService;
  private final MeetupWorkflows meetupWorkflows;
  private final AdminWorkflows adminWorkflows;
  private final LocaleService localeService;
  private final UserWorkflows userWorkflows;
  private final RegisteredUserDao userDao;
  private final GameDao gameDao;

  public ContactSection contactSection(RegisteredUser currentUser, Runnable runnable) {
    return new ContactSection(
        this, contactInfoService, userWorkflows, userDao, currentUser, runnable);
  }

  public LocalePicker localePicker() {
    return new LocalePicker(localeService);
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
    return new AddGameDialog(list, currentUser, saveConsumer);
  }

  public MessengerLinkDialog messengerLinkDialog(
      UUID currentUserId, ContactInfoType type, Runnable runnable) {
    MessengerLinkContext ctx = verificationService.buildLinkContext(currentUserId, type);
    return new MessengerLinkDialog(ctx, currentUserId, userDao, runnable);
  }

  public AttendeeRequestCard attendeeRequestCard(
      MeetupAttendeesContext ctx,
      MeetupAttendeesContext.AttendeeRequestItem item,
      Runnable runnable) {
    return new AttendeeRequestCard(
        ctx,
        item,
        authService.isCurrentUserAdmin(),
        userDao,
        meetupWorkflows,
        adminWorkflows,
        localeService,
        runnable);
  }
}

package org.bytewright.bgmo.domain.service.notification;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.notification.NotificationContext;
import org.bytewright.bgmo.domain.model.notification.NotificationTargetType;
import org.bytewright.bgmo.domain.model.notification.NotificationType;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.model.user.UserRole;
import org.bytewright.bgmo.domain.service.UrlGenerator;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.springframework.stereotype.Service;

/**
 * Central hub for pushing notifications about events to user. This will check implemented contact
 * options, user preferred method of contact and then trigger message delivery
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationManager {
  private final List<NotificationTaskExecutor> executors;
  private final UrlGenerator urlGenerator;
  private final RegisteredUserDao userDao;
  private final MeetupDao meetupDao;

  public void addNewEventCreatedTask(UUID meetupId) {
    var meetup = meetupDao.findById(meetupId).orElseThrow();
    var context =
        NotificationContext.builder()
            .notificationType(NotificationType.NEW_EVENT)
            .messageKey("notification.newMeetup")
            .messageArg(meetup.getTitle())
            .messageArg(urlGenerator.getUrlFor(meetup))
            .notificationTargetType(NotificationTargetType.GROUP)
            .meetupId(meetupId)
            .build();

    dispatch(context);
  }

  public void addUserApprovedTask(UUID userId) {
    // todo add async task to notify user via primary channel that the account is active
  }

  public void addNewJoinRequestCreatedTask(UUID meetupId, UUID requestId) {
    // todo add async task to notify event creator that a new request was created
  }

  public void addJoinRequestApprovedTask(UUID meetupId, UUID requestId, UUID userId) {
    // todo add async task to notify user that a request was accepted
  }

  public void addEventRescheduledTask(UUID meetupId) {
    // todo add async task to notify users that event was moved
  }

  public void addEventCanceledTask(UUID meetupId) {
    // todo add async task to notify users that event was canceled
  }

  public void addUserRegistrationTask(UUID userId) {
    Set<RegisteredUser> siteAdmins = userDao.findAllActiveByRole(UserRole.ADMIN);
    RegisteredUser newUser = userDao.findOrThrow(userId);
    log.info(
        "Sending info about new user registration with id {} to {} admins", userId, siteAdmins);
    for (RegisteredUser user : siteAdmins) {
      var context =
          NotificationContext.builder()
              .notificationType(NotificationType.USER_REGISTRATION)
              .messageKey("notification.newRegistration")
              .messageArg(newUser.getDisplayName())
              .notificationTargetType(NotificationTargetType.DIRECT)
              .userId(user.getId())
              .build();
      dispatchToPrimary(user, context);
    }
  }

  private void dispatch(NotificationContext context) {
    executors.stream().filter(e -> e.supports(context)).forEach(e -> e.execute(context));
  }

  private void dispatchToPrimary(RegisteredUser user, NotificationContext context) {
    Optional<ContactInfo> primaryContactOpt = getPrimaryContact(user);
    if (primaryContactOpt.isPresent()) {
      ContactInfo contactInfo = primaryContactOpt.get();
      executors.stream()
          .filter(e -> e.isContactHandlerFor(contactInfo.type()))
          .filter(e -> e.supports(context))
          .findAny()
          .ifPresent(notificationTaskExecutor -> notificationTaskExecutor.execute(context));
    }
  }

  private Optional<ContactInfo> getPrimaryContact(RegisteredUser user) {
    UUID primaryContactId = user.getPrimaryContactId();
    if (primaryContactId != null) {
      return user.getContactInfos().stream()
          .filter(contactInfo -> primaryContactId.equals(contactInfo.id()))
          .findAny();
    }
    if (user.getContactInfos().size() == 1) {
      return user.getContactInfos().stream().findAny();
    }
    return Optional.empty();
  }
}

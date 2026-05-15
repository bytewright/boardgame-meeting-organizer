package org.bytewright.bgmo.domain.service.notification;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.domain.model.event.ModelUpdatedEvents;
import org.bytewright.bgmo.domain.model.notification.NotificationContext;
import org.bytewright.bgmo.domain.model.notification.NotificationTargetType;
import org.bytewright.bgmo.domain.model.notification.NotificationType;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.model.user.UserRole;
import org.bytewright.bgmo.domain.service.UrlGenerator;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.bytewright.bgmo.domain.service.data.ModelDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Central hub for pushing notifications about events to user. This will check implemented contact
 * options, user preferred method of contact and then trigger message delivery
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationManager {
  private final ModelDao<MeetupJoinRequest> joinRequestDao;
  private final List<NotificationTaskExecutor> executors;
  private final UrlGenerator urlGenerator;
  private final RegisteredUserDao userDao;
  private final MeetupDao meetupDao;

  @EventListener
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void onMeetupCreatedEvent(ModelUpdatedEvents.MeetupCreated event) {
    UUID meetupId = event.id();
    var meetup = meetupDao.findById(meetupId).orElseThrow();
    var context =
        NotificationContext.builder()
            .notificationType(NotificationType.EVENT_NEW)
            .messageKey("notification.meetup.created")
            .messageArg(meetup.getTitle())
            .messageArg(urlGenerator.getUrlFor(meetup))
            .notificationTargetType(NotificationTargetType.GROUP)
            .meetupId(meetupId)
            .build();

    dispatch(context);
  }

  @EventListener
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void onJoinRequestCreated(ModelUpdatedEvents.JoinRequestCreated event) {
    MeetupJoinRequest joinRequest = joinRequestDao.findOrThrow(event.id());
    MeetupEvent meetup = meetupDao.findOrThrow(joinRequest.getMeetupId());
    RegisteredUser eventCreator = userDao.findOrThrow(meetup.getCreatorId());
    var context =
        NotificationContext.builder()
            .notificationType(NotificationType.JOIN_REQUEST_CREATED)
            .messageKey("notification.newJoinRequest")
            .messageArg(joinRequest.getDisplayName())
            .messageArg(meetup.getTitle())
            .messageArg(urlGenerator.getUrlFor(meetup))
            .notificationTargetType(NotificationTargetType.DIRECT)
            .userId(eventCreator.getId())
            .locale(eventCreator.getPreferredLocale())
            .meetupId(meetup.id())
            .build();
    dispatch(context);
  }

  @EventListener
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void onJoinRequestApproved(ModelUpdatedEvents.JoinRequestApproved event) {
    MeetupJoinRequest joinRequest = joinRequestDao.findOrThrow(event.id());
    MeetupEvent meetup = meetupDao.findOrThrow(joinRequest.getMeetupId());
    Optional.ofNullable(joinRequest.getUserId())
        .flatMap(userDao::find)
        .ifPresent(
            joiner -> {
              var context =
                  NotificationContext.builder()
                      .notificationType(NotificationType.JOIN_REQUEST_APPROVED)
                      .messageKey("notification.joinRequestApproved")
                      .messageArg(meetup.getEventDate())
                      .messageArg(meetup.getTitle())
                      .messageArg(urlGenerator.getUrlFor(meetup))
                      .notificationTargetType(NotificationTargetType.DIRECT)
                      .userId(joiner.getId())
                      .locale(joiner.getPreferredLocale())
                      .meetupId(meetup.id())
                      .build();
              dispatch(context);
            });
  }

  @EventListener
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void addMeetupRescheduled(ModelUpdatedEvents.MeetupRescheduled event) {
    UUID meetupId = event.id();
    var meetup = meetupDao.findById(meetupId).orElseThrow();
    for (MeetupJoinRequest joinRequest : meetup.getJoinRequests()) {
      RegisteredUser joiner = userDao.findOrThrow(joinRequest.getUserId());
      var context =
          NotificationContext.builder()
              .notificationType(NotificationType.EVENT_RESCHEDULED)
              .messageKey("notification.meetup.rescheduled")
              .messageArg(meetup.getTitle())
              .messageArg(meetup.getEventDate())
              .messageArg(urlGenerator.getUrlFor(meetup))
              .notificationTargetType(NotificationTargetType.DIRECT)
              .meetupId(meetupId)
              .userId(joiner.getId())
              .locale(joiner.getPreferredLocale())
              .build();
      dispatch(context);
    }
  }

  @EventListener
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void onMeetupCanceled(ModelUpdatedEvents.MeetupCanceled event) {
    UUID meetupId = event.id();
    var meetup = meetupDao.findById(meetupId).orElseThrow();
    for (MeetupJoinRequest joinRequest : meetup.getJoinRequests()) {
      RegisteredUser joiner = userDao.findOrThrow(joinRequest.getUserId());
      var context =
          NotificationContext.builder()
              .notificationType(NotificationType.EVENT_CANCELED)
              .messageKey("notification.meetup.canceled")
              .messageArg(meetup.getTitle())
              .messageArg(urlGenerator.getUrlFor(meetup))
              .notificationTargetType(NotificationTargetType.DIRECT)
              .meetupId(meetupId)
              .userId(joiner.getId())
              .locale(joiner.getPreferredLocale())
              .build();
      dispatch(context);
    }
  }

  @EventListener
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void onUserCreated(ModelUpdatedEvents.UserCreated event) {
    UUID userId = event.id();
    Set<RegisteredUser> siteAdmins = userDao.findAllActiveByRole(UserRole.ADMIN);
    RegisteredUser newUser = userDao.findOrThrow(userId);
    log.info(
        "Sending info about new user registration with id {} to {} admins",
        userId,
        siteAdmins.size());
    for (RegisteredUser user : siteAdmins) {
      var context =
          NotificationContext.builder()
              .notificationType(NotificationType.USER_REGISTRATION)
              .messageKey("notification.user.new")
              .messageArg(newUser.getDisplayName())
              .notificationTargetType(NotificationTargetType.DIRECT)
              .userId(user.getId())
              .build();
      dispatchToPrimary(user, context);
    }
  }

  @EventListener
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void onUserVerified(ModelUpdatedEvents.UserVerified event) {
    RegisteredUser user = userDao.findOrThrow(event.id());
    var context =
        NotificationContext.builder()
            .notificationType(NotificationType.EVENT_CANCELED)
            .messageKey("notification.user.approved")
            .notificationTargetType(NotificationTargetType.DIRECT)
            .userId(user.getId())
            .locale(user.getPreferredLocale())
            .build();
    dispatch(context);
  }

  private void dispatch(NotificationContext context) {
    executors.stream().filter(e -> e.supports(context)).forEach(e -> e.execute(context));
  }

  private void dispatchToPrimary(RegisteredUser user, NotificationContext context) {
    Optional<ContactInfo> primaryContactOpt = getPrimaryContact(user);
    if (primaryContactOpt.isPresent()) {
      ContactInfo contactInfo = primaryContactOpt.get();
      log.info(
          "Dispatching {} notification to user {} over channel {}",
          context.notificationType(),
          user.logEntity(),
          contactInfo.type());
      executors.stream()
          .filter(e -> e.isContactHandlerFor(contactInfo.type()))
          .filter(e -> e.supports(context))
          .findAny()
          .ifPresent(notificationTaskExecutor -> notificationTaskExecutor.execute(context));
    } else {
      log.info(
          "Couldn't find primary contact info to dispatch '{}' to user: {}",
          context.notificationType(),
          user.logEntity());
    }
  }

  private Optional<ContactInfo> getPrimaryContact(RegisteredUser user) {
    UUID primaryContactId = user.getPrimaryContactId();
    if (primaryContactId != null) {
      return user.getContactInfos().stream()
          .filter(contactInfo -> primaryContactId.equals(contactInfo.id()))
          .findAny();
    }
    Set<ContactInfoType> activeTypes = Set.of(ContactInfoType.TELEGRAM);
    if (user.getContactInfos().size() == 1) {
      return user.getContactInfos().stream()
          .filter(contactInfo -> activeTypes.contains(contactInfo.type()))
          .findAny();
    }
    return Optional.empty();
  }
}

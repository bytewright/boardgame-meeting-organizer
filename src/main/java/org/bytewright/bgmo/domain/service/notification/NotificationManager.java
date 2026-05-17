package org.bytewright.bgmo.domain.service.notification;

import jakarta.transaction.Transactional;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.domain.model.event.ModelUpdatedEvents;
import org.bytewright.bgmo.domain.model.notification.NotificationContext;
import org.bytewright.bgmo.domain.model.notification.NotificationPayload;
import org.bytewright.bgmo.domain.model.notification.NotificationTargetType;
import org.bytewright.bgmo.domain.model.user.*;
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
  private final Set<NotificationTaskExecutor> executors = new HashSet<>();
  private final ModelDao<MeetupJoinRequest> joinRequestDao;
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
            .notificationTargetType(NotificationTargetType.GROUP)
            .payload(
                NotificationPayload.MeetupCreated.builder()
                    .title(meetup.getTitle())
                    .meetupId(meetupId)
                    .meetupUrl(urlGenerator.getUrlFor(meetup))
                    .build())
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
            .notificationTargetType(NotificationTargetType.DIRECT)
            .payload(
                NotificationPayload.JoinRequestCreated.builder()
                    .requesterName(joinRequest.getDisplayName())
                    .joinRequestId(joinRequest.getId())
                    .title(meetup.getTitle())
                    .meetupUrl(urlGenerator.getUrlFor(meetup))
                    .build())
            .userId(eventCreator.getId())
            .locale(eventCreator.getPreferredLocale())
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
                      .notificationTargetType(NotificationTargetType.DIRECT)
                      .payload(
                          NotificationPayload.JoinRequestApproved.builder()
                              .eventDate(meetup.getEventDate())
                              .title(meetup.getTitle())
                              .joinRequestId(joinRequest.getId())
                              .meetupUrl(urlGenerator.getUrlFor(meetup))
                              .build())
                      .userId(joiner.getId())
                      .locale(joiner.getPreferredLocale())
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
              .notificationTargetType(NotificationTargetType.DIRECT)
              .payload(
                  NotificationPayload.MeetupRescheduled.builder()
                      .title(meetup.getTitle())
                      .meetupId(meetupId)
                      .newEventDate(meetup.getEventDate())
                      .meetupUrl(urlGenerator.getUrlFor(meetup))
                      .build())
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
              .notificationTargetType(NotificationTargetType.DIRECT)
              .payload(
                  NotificationPayload.MeetupCanceled.builder()
                      .title(meetup.getTitle())
                      .meetupId(meetupId)
                      .meetupUrl(urlGenerator.getUrlFor(meetup))
                      .build())
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
              .notificationTargetType(NotificationTargetType.DIRECT)
              .payload(
                  NotificationPayload.UserRegistration.builder()
                      .username(newUser.getDisplayName())
                      .build())
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
            .notificationTargetType(NotificationTargetType.DIRECT)
            .userId(user.getId())
            .locale(user.getPreferredLocale())
            .build();
    dispatch(context);
  }

  void registerTaskExecutors(Collection<NotificationTaskExecutor> executors) {
    this.executors.addAll(executors);
  }

  public void dispatch(NotificationContext context) {
    executors.stream().filter(e -> e.supports(context)).forEach(e -> e.execute(context));
  }

  private void dispatchToPrimary(RegisteredUser user, NotificationContext context) {
    Optional<ContactOption> primaryContactOpt = getPrimaryContact(user);
    if (primaryContactOpt.isPresent()) {
      ContactOption contact = primaryContactOpt.get();
      log.info(
          "Dispatching {} notification to user {} over channel {}",
          context.payload().getClass().getSimpleName(),
          user.logEntity(),
          contact.getType());
      executors.stream()
          .filter(e -> e.isContactHandlerFor(contact.getType()))
          .filter(e -> e.supports(context))
          .findAny()
          .ifPresent(notificationTaskExecutor -> notificationTaskExecutor.execute(context));
    } else {
      log.info(
          "Couldn't find primary contact info to dispatch '{}' to user: {}",
          context.payload().getClass().getSimpleName(),
          user.logEntity());
    }
  }

  private Optional<ContactOption> getPrimaryContact(RegisteredUser user) {
    UUID primaryContactId = user.getPrimaryContactId();
    if (primaryContactId != null) {
      return user.getContactOptions().stream()
          .filter(contact -> primaryContactId.equals(contact.id()))
          .findAny();
    }
    Set<ContactInfoType> activeTypes = Set.of(ContactInfoType.TELEGRAM);
    if (user.getContactOptions().size() == 1) {
      return user.getContactOptions().stream()
          .filter(contactInfo -> activeTypes.contains(contactInfo.getType()))
          .findAny();
    }
    return Optional.empty();
  }
}

package org.bytewright.bgmo.domain.service.notification;

import jakarta.transaction.Transactional;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.JoinRequestPayload;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.domain.model.event.ModelUpdatedEvents;
import org.bytewright.bgmo.domain.model.notification.NotificationContext;
import org.bytewright.bgmo.domain.model.notification.NotificationPayload;
import org.bytewright.bgmo.domain.model.user.*;
import org.bytewright.bgmo.domain.service.SiteManagementService;
import org.bytewright.bgmo.domain.service.UrlGenerator;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.bytewright.bgmo.domain.service.data.ModelDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.domain.service.user.ContactInfoService;
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
  private final SiteManagementService siteManagementService;
  private final ContactInfoService contactInfoService;
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
            .target(NotificationContext.Target.Group.builder().build())
            .payload(
                NotificationPayload.MeetupCreated.builder()
                    .title(meetup.getTitle())
                    .meetupId(meetupId)
                    .meetupUrl(urlGenerator.getUrlFor(meetup))
                    .build())
            .locale(siteManagementService.getDefaultLocale())
            .build();

    dispatch(context);
  }

  @EventListener
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void onJoinRequestCreated(ModelUpdatedEvents.JoinRequestCreated event) {
    MeetupJoinRequest joinRequest = joinRequestDao.findOrThrow(event.id());
    MeetupEvent meetup = meetupDao.findOrThrow(joinRequest.getMeetupId());
    RegisteredUser eventCreator = userDao.findOrThrow(meetup.getCreatorId());
    String displayName =
        switch (joinRequest.getPayload()) {
          case JoinRequestPayload.Anon anon -> anon.displayName();
          case JoinRequestPayload.AnonEmail anonEmail -> anonEmail.displayName();
          case JoinRequestPayload.User user -> {
            RegisteredUser joiner = userDao.findOrThrow(user.userId());
            yield joiner.getDisplayName();
          }
        };
    var context =
        NotificationContext.builder()
            .target(
                NotificationContext.Target.User.builder()
                    .userId(eventCreator.id())
                    .displayName(eventCreator.getDisplayName())
                    .primaryContactInfo(
                        contactInfoService
                            .getPrimaryContact(eventCreator)
                            .map(ContactOption::getContactInfo)
                            .orElseThrow())
                    .build())
            .payload(
                NotificationPayload.JoinRequestCreated.builder()
                    .requesterName(displayName)
                    .joinRequestId(joinRequest.getId())
                    .title(meetup.getTitle())
                    .meetupUrl(urlGenerator.getUrlFor(meetup))
                    .build())
            .locale(eventCreator.getPreferredLocale())
            .build();
    dispatch(context);
  }

  @EventListener
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void onJoinRequestApproved(ModelUpdatedEvents.JoinRequestApproved event) {
    MeetupJoinRequest joinRequest = joinRequestDao.findOrThrow(event.id());
    MeetupEvent meetup = meetupDao.findOrThrow(joinRequest.getMeetupId());
    var payload =
        NotificationPayload.JoinRequestApproved.builder()
            .eventDate(meetup.getEventDate())
            .title(meetup.getTitle())
            .joinRequestId(joinRequest.getId())
            .meetupUrl(urlGenerator.getUrlFor(meetup))
            .build();
    switch (joinRequest.getPayload()) {
      case JoinRequestPayload.User user -> {
        RegisteredUser joiner = userDao.findOrThrow(user.userId());
        var context =
            NotificationContext.builder()
                .target(
                    NotificationContext.Target.User.builder()
                        .userId(joiner.id())
                        .displayName(joiner.getDisplayName())
                        .primaryContactInfo(
                            contactInfoService
                                .getPrimaryContact(joiner)
                                .map(ContactOption::getContactInfo)
                                .orElseThrow())
                        .build())
                .payload(payload)
                .locale(joiner.getPreferredLocale())
                .build();
        dispatch(context);
      }
      case JoinRequestPayload.AnonEmail anonEmail -> {
        var context =
            NotificationContext.builder()
                .target(
                    NotificationContext.Target.Anon.builder()
                        .contactInfo(anonEmail.emailContact())
                        .displayName(anonEmail.displayName())
                        .build())
                .payload(payload)
                .locale(siteManagementService.getDefaultLocale())
                .build();
        dispatch(context);
      }
      case JoinRequestPayload.Anon ignored ->
          log.info("Can't dispatch request approval notification to anon user");
    }
  }

  @EventListener
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void addMeetupRescheduled(ModelUpdatedEvents.MeetupRescheduled event) {
    UUID meetupId = event.id();
    var meetup = meetupDao.findById(meetupId).orElseThrow();
    var payload =
        NotificationPayload.MeetupRescheduled.builder()
            .title(meetup.getTitle())
            .meetupId(meetupId)
            .newEventDate(meetup.getEventDate())
            .meetupUrl(urlGenerator.getUrlFor(meetup))
            .build();
    for (MeetupJoinRequest joinRequest : meetup.getJoinRequests()) {
      switch (joinRequest.getPayload()) {
        case JoinRequestPayload.User user -> {
          RegisteredUser joiner = userDao.findOrThrow(user.userId());
          var context =
              NotificationContext.builder()
                  .target(
                      NotificationContext.Target.User.builder()
                          .userId(joiner.id())
                          .displayName(joiner.getDisplayName())
                          .primaryContactInfo(
                              contactInfoService
                                  .getPrimaryContact(joiner)
                                  .map(ContactOption::getContactInfo)
                                  .orElseThrow())
                          .build())
                  .payload(payload)
                  .locale(joiner.getPreferredLocale())
                  .build();
          dispatch(context);
        }
        case JoinRequestPayload.Anon ignored ->
            log.info("Can't dispatch meeting rescheduled notification to anon user");
        case JoinRequestPayload.AnonEmail anonEmail -> {
          var context =
              NotificationContext.builder()
                  .target(
                      NotificationContext.Target.Anon.builder()
                          .contactInfo(anonEmail.emailContact())
                          .displayName(anonEmail.displayName())
                          .build())
                  .payload(payload)
                  .locale(siteManagementService.getDefaultLocale())
                  .build();
          dispatch(context);
        }
      }
    }
  }

  @EventListener
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void onMeetupCanceled(ModelUpdatedEvents.MeetupCanceled event) {
    UUID meetupId = event.id();
    var meetup = meetupDao.findById(meetupId).orElseThrow();
    var payload =
        NotificationPayload.MeetupCanceled.builder()
            .title(meetup.getTitle())
            .meetupId(meetupId)
            .meetupUrl(urlGenerator.getUrlFor(meetup))
            .build();
    for (MeetupJoinRequest joinRequest : meetup.getJoinRequests()) {
      switch (joinRequest.getPayload()) {
        case JoinRequestPayload.User user -> {
          RegisteredUser joiner = userDao.findOrThrow(user.userId());
          var context =
              NotificationContext.builder()
                  .target(
                      NotificationContext.Target.User.builder()
                          .userId(joiner.id())
                          .displayName(joiner.getDisplayName())
                          .primaryContactInfo(
                              contactInfoService
                                  .getPrimaryContact(joiner)
                                  .map(ContactOption::getContactInfo)
                                  .orElseThrow())
                          .build())
                  .payload(payload)
                  .locale(joiner.getPreferredLocale())
                  .build();
          dispatch(context);
        }
        case JoinRequestPayload.Anon ignored ->
            log.info("Can't dispatch meeting canceled notification to anon user");
        case JoinRequestPayload.AnonEmail anonEmail -> {
          var context =
              NotificationContext.builder()
                  .target(
                      NotificationContext.Target.Anon.builder()
                          .contactInfo(anonEmail.emailContact())
                          .displayName(anonEmail.displayName())
                          .build())
                  .payload(payload)
                  .locale(siteManagementService.getDefaultLocale())
                  .build();
          dispatch(context);
        }
      }
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
              .target(
                  NotificationContext.Target.User.builder()
                      .userId(user.id())
                      .displayName(user.getDisplayName())
                      .primaryContactInfo(
                          contactInfoService
                              .getPrimaryContact(user)
                              .map(ContactOption::getContactInfo)
                              .orElseThrow())
                      .build())
              .payload(
                  NotificationPayload.UserRegistration.builder()
                      .username(newUser.getDisplayName())
                      .build())
              .locale(user.getPreferredLocale())
              .build();
      dispatchToPrimary(user, context);
    }
  }

  void registerTaskExecutors(Collection<NotificationTaskExecutor> executors) {
    this.executors.addAll(executors);
  }

  public void dispatch(NotificationContext context) {
    executors.stream().filter(e -> e.supports(context)).forEach(e -> e.execute(context));
  }

  private void dispatchToPrimary(RegisteredUser user, NotificationContext context) {
    Optional<ContactOption> primaryContactOpt = contactInfoService.getPrimaryContact(user);
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
}

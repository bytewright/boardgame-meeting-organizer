package org.bytewright.bgmo.usecases;

import jakarta.transaction.Transactional;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.MeetupCreation;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.domain.model.RequestState;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.BgmoProperties;
import org.bytewright.bgmo.domain.service.automation.TimeSource;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.bytewright.bgmo.domain.service.data.ModelDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.domain.service.notification.NotificationManager;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MeetupWorkflows {
  private final NotificationManager notificationManager;
  private final ModelDao<MeetupJoinRequest> joinRequestModelDao;
  private final BgmoProperties bgmoProperties;
  private final RegisteredUserDao userDao;
  private final MeetupDao meetupDao;
  private final TimeSource timeSource;

  public MeetupEvent create(MeetupCreation event) {
    if (!event.isUnlimitedSlots() && event.getJoinSlots() == null) {
      log.warn(
          "Detected missconfig, changing new event to unlimited slots as no slot count was provided");
      event.setUnlimitedSlots(true);
    }
    log.info("Creating new meetup from: {}", event);
    MeetupEvent meetupEvent =
        MeetupEvent.builder()
            .title(event.getTitle())
            .description(event.getDescription())
            .eventDate(event.getEventDate())
            .registrationClosing(event.getRegistrationClosingDate())
            .creatorId(event.getCreator().getId())
            .canceled(false)
            .tsCreation(timeSource.now())
            .joinSlots(event.getJoinSlots() != null ? event.getJoinSlots() : -1)
            .unlimitedSlots(event.isUnlimitedSlots())
            .allowAnonSignup(event.isAllowAnonSignup())
            .durationHours(event.getDurationHours())
            .offeredGames(event.getOfferedGames())
            .build();
    MeetupEvent persisted = meetupDao.createOrUpdate(meetupEvent);
    notificationManager.addNewEventCreatedTask(persisted.id());
    log.info("Available at: {}meetup/{}", bgmoProperties.getBaseUrl(), persisted.id());
    return persisted;
  }

  public void requestToJoin(UUID meetupId, UUID userId, String comment) {
    // todo comment will be added later to request
    MeetupEvent meetupEvent = meetupDao.findOrThrow(meetupId);
    Optional<MeetupJoinRequest> existingRequest =
        meetupEvent.getJoinRequests().stream()
            .filter(r -> Objects.equals(r.getUserId(), userId))
            .findAny();
    if (existingRequest.isPresent()) {
      MeetupJoinRequest request = existingRequest.get();
      if (request.getRequestState() == RequestState.CANCELED) {
        transitionToRequested(request);
        notificationManager.addNewJoinRequestCreatedTask(meetupEvent.id(), request.id());
      } else {
        log.info("User {} tried to join meeting he already has requested to join...", userId);
      }
      return;
    }
    RegisteredUser user = userDao.findOrThrow(userId);
    var request =
        MeetupJoinRequest.builder()
            .meetupId(meetupEvent.getId())
            .userId(user.getId())
            .displayName(user.getDisplayName())
            .tsCreation(timeSource.now())
            .build();
    meetupEvent.getJoinRequests().add(request);
    log.info(
        "Added join request from user {} to event: {}", user.getId(), meetupEvent.logIdentity());
    UUID requestId =
        meetupDao.createOrUpdate(meetupEvent).getJoinRequests().stream()
            .filter(meetupJoinRequest -> user.getId().equals(meetupJoinRequest.getUserId()))
            .findAny()
            .map(MeetupJoinRequest::getId)
            .orElseThrow();
    notificationManager.addNewJoinRequestCreatedTask(meetupEvent.id(), requestId);
  }

  public void requestToJoinAnon(
      UUID meetupId, UUID anonToken, String displayName, String contactInfo) {
    MeetupEvent meetupEvent = meetupDao.findOrThrow(meetupId);
    Optional<MeetupJoinRequest> existingRequest =
        meetupEvent.getJoinRequests().stream()
            .filter(meetupJoinRequest -> anonToken.equals(meetupJoinRequest.getAnonToken()))
            .findAny();
    if (existingRequest.isPresent()) {
      MeetupJoinRequest request = existingRequest.get();
      if (request.getRequestState() == RequestState.CANCELED) {
        transitionToRequested(request);
        notificationManager.addNewJoinRequestCreatedTask(meetupEvent.id(), request.id());
      } else {
        log.info(
            "Anon '{}' tried to join meeting he already has requested to join...", displayName);
      }
      return;
    }
    var request =
        MeetupJoinRequest.builder()
            .meetupId(meetupEvent.getId())
            .anonToken(anonToken)
            .displayName(displayName)
            .contactInfo(contactInfo)
            .tsCreation(timeSource.now())
            .build();
    meetupEvent.getJoinRequests().add(request);
    log.info("Added join request from anon user to event: {}", meetupEvent.logIdentity());
    UUID requestId =
        meetupDao.createOrUpdate(meetupEvent).getJoinRequests().stream()
            .filter(meetupJoinRequest -> anonToken.equals(meetupJoinRequest.getAnonToken()))
            .findAny()
            .map(MeetupJoinRequest::getId)
            .orElseThrow();
    notificationManager.addNewJoinRequestCreatedTask(meetupEvent.id(), requestId);
  }

  public MeetupEvent confirmAttendee(UUID meetupId, MeetupJoinRequest joinRequest) {
    MeetupEvent meetupEvent = meetupDao.findOrThrow(meetupId);
    if (meetupEvent.getJoinRequests().stream().noneMatch(r -> r.equals(joinRequest))) {
      throw new IllegalArgumentException(
          "Given join request is not persisted with meeting! " + joinRequest);
    }
    if (isFull(meetupEvent)) {
      throw new IllegalStateException(
          "Meeting is already full, can't confirm more requests: " + meetupEvent.logIdentity());
    }
    transitionToAccepted(joinRequest);
    notificationManager.addJoinRequestApprovedTask(
        meetupEvent.id(), joinRequest.id(), joinRequest.getUserId());
    return meetupDao.findOrThrow(meetupId);
  }

  private void transitionToRequested(MeetupJoinRequest request) {
    if (request.getRequestState() == RequestState.ACCEPTED) {
      log.warn("Can't transition from accepted to open! {}", request);
      return;
    }
    request.setRequestState(RequestState.OPEN);
    joinRequestModelDao.createOrUpdate(request);
  }

  private void transitionToAccepted(MeetupJoinRequest joinRequest) {
    if (joinRequest.getRequestState() != RequestState.CANCELED) {
      joinRequest.setRequestState(RequestState.ACCEPTED);
    }
    joinRequestModelDao.createOrUpdate(joinRequest);
  }

  public boolean isFull(MeetupEvent meetup) {
    long acceptedCount =
        meetup.getJoinRequests().stream()
            .filter(r -> RequestState.ACCEPTED == r.getRequestState())
            .count();
    return !meetup.isUnlimitedSlots() && acceptedCount == meetup.getJoinSlots();
  }

  public void cancelJoinRequest(UUID joinRequestId) {
    MeetupJoinRequest request = joinRequestModelDao.findOrThrow(joinRequestId);
    log.info("User canceled join request for meeting {}", request.getMeetupId());
    transitionToCanceled(request);
  }

  private void transitionToCanceled(MeetupJoinRequest request) {
    if (request.getRequestState() == RequestState.CANCELED) {
      return;
    }
    request.setRequestState(RequestState.CANCELED);
    joinRequestModelDao.createOrUpdate(request);
  }

  public void cancelMeetup(UUID meetupId) {
    MeetupEvent meetup = meetupDao.findOrThrow(meetupId);
    log.info("Creator wants to cancel meeting: {}", meetup);
    meetup.setCanceled(true);
    meetupDao.createOrUpdate(meetup);
    notificationManager.addEventCanceledTask(meetup.getId());
  }

  public void rescheduleEvent(UUID meetupId, ZonedDateTime newDate, ZonedDateTime newRegClose) {
    MeetupEvent meetup = meetupDao.findOrThrow(meetupId);
    log.info("Creator wants to reschedule meeting: {}", meetup.getId());
    meetup.setEventDate(newDate);
    meetup.setRegistrationClosing(newRegClose);
    meetupDao.createOrUpdate(meetup);
    notificationManager.addEventRescheduledTask(meetup.getId());
  }

  public int confirmRemainingSlotsRandom(UUID meetupId) {
    MeetupEvent meetup = meetupDao.findOrThrow(meetupId);
    List<MeetupJoinRequest> openRequests =
        meetup.getJoinRequests().stream()
            .filter(r -> r.getRequestState() == RequestState.OPEN)
            .collect(Collectors.toCollection(ArrayList::new));

    if (openRequests.isEmpty()) {
      return 0;
    }

    Collections.shuffle(openRequests);

    long accepted =
        meetup.getJoinRequests().stream()
            .filter(r -> r.getRequestState() == RequestState.ACCEPTED)
            .count();
    int remainingSlots = (int) (meetup.getJoinSlots() - accepted);
    int slotsFilledAtRandom = Math.min(remainingSlots, openRequests.size());
    List<MeetupJoinRequest> toConfirm = openRequests.subList(0, slotsFilledAtRandom);

    toConfirm.forEach(r -> confirmAttendee(meetup.getId(), r));
    return slotsFilledAtRandom;
  }
}

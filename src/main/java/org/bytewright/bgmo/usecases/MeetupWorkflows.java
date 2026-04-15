package org.bytewright.bgmo.usecases;

import jakarta.transaction.Transactional;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.MeetupCreation;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.domain.model.RequestState;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.automation.TimeSource;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.bytewright.bgmo.domain.service.data.ModelDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MeetupWorkflows {
  private final ModelDao<MeetupJoinRequest> joinRequestModelDao;
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
    log.info("Available at: http://localhost:8080/meetup/{}", persisted.id());
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
    meetupDao.createOrUpdate(meetupEvent);
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
    meetupDao.createOrUpdate(meetupEvent);
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
}

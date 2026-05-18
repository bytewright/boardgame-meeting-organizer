package org.bytewright.bgmo.usecases;

import jakarta.transaction.Transactional;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.*;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.InputSanitizer;
import org.bytewright.bgmo.domain.service.SiteManagementService;
import org.bytewright.bgmo.domain.service.automation.TimeSource;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.bytewright.bgmo.domain.service.data.ModelDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.domain.service.event.EventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MeetupWorkflows {
  private final SlotDistributionWorkflows slotDistributionWorkflows;
  private final ModelDao<MeetupJoinRequest> joinRequestModelDao;
  private final SiteManagementService siteManagementService;
  private final EventPublisher eventPublisher;
  private final InputSanitizer inputSanitizer;
  private final RegisteredUserDao userDao;
  private final TimeSource timeSource;
  private final MeetupDao meetupDao;

  public MeetupEvent create(MeetupEvent.MeetupCreation event) {
    if (!event.isUnlimitedSlots() && event.getJoinSlots() == null) {
      log.warn(
          "Detected missconfig, changing new event to unlimited slots as no slot count was provided");
      event.setUnlimitedSlots(true);
    }
    log.info("Creating new meetup from: {}", event);
    RegisteredUser creator = event.getCreator();
    if (creator.getContactOptions().isEmpty()) {
      throw new IllegalArgumentException("Can't create a meeting without any contact options!");
    }
    ZonedDateTime registrationClosing =
        ZonedDateTime.of(
            event.getRegistrationClosingDate(),
            LocalTime.of(20, 0),
            siteManagementService.getServiceTimeZone());
    MeetupEvent meetupEvent =
        MeetupEvent.builder()
            .title(inputSanitizer.plainText(event.getTitle()))
            .description(inputSanitizer.plainText(event.getDescription()))
            .eventDate(event.getEventDate())
            .registrationClosing(registrationClosing)
            .creatorId(creator.getId())
            .canceled(false)
            .tsCreation(timeSource.now())
            .joinSlots(event.getJoinSlots() != null ? event.getJoinSlots() : -1)
            .unlimitedSlots(event.isUnlimitedSlots())
            .allowAnonSignup(event.isAllowAnonSignup())
            .durationHours(event.getDurationHours())
            .offeredGames(event.getOfferedGames())
            .areaHint(inputSanitizer.plainText(event.getLocation().areaHint().trim()))
            .fullLocation(inputSanitizer.plainText(event.getLocation().fullLocation().trim()))
            .visibility(event.getVisibility())
            .slotStrategy(event.getSlotStrategy())
            .build();
    MeetupEvent persisted = meetupDao.createOrUpdate(meetupEvent);
    eventPublisher.publishMeetupCreatedAfterTransaction(persisted);
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
        if (meetupEvent.getSlotStrategy() == SlotDistributionStrategy.FIRST_COME_FIRST_SERVE) {
          slotDistributionWorkflows.handleNewJoinRequestFCFS(meetupEvent, request);
        }
        eventPublisher.publishJoinRequestCreatedAfterTransaction(request.id());
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
            .comment(comment)
            .build();
    MeetupJoinRequest joinRequest = joinRequestModelDao.createOrUpdate(request);
    log.info(
        "Added join request from user {} to event: {}", user.getId(), meetupEvent.logIdentity());
    UUID requestId = joinRequest.getId();
    if (meetupEvent.getSlotStrategy() == SlotDistributionStrategy.FIRST_COME_FIRST_SERVE) {
      slotDistributionWorkflows.handleNewJoinRequestFCFS(meetupEvent, request);
    }
    eventPublisher.publishJoinRequestCreatedAfterTransaction(requestId);
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
        if (meetupEvent.getSlotStrategy() == SlotDistributionStrategy.FIRST_COME_FIRST_SERVE) {
          slotDistributionWorkflows.handleNewJoinRequestFCFS(meetupEvent, request);
        }
        eventPublisher.publishJoinRequestCreatedAfterTransaction(request.id());
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
    MeetupJoinRequest persistedRequest =
        meetupDao.createOrUpdate(meetupEvent).getJoinRequests().stream()
            .filter(meetupJoinRequest -> anonToken.equals(meetupJoinRequest.getAnonToken()))
            .findAny()
            .orElseThrow();
    if (meetupEvent.getSlotStrategy() == SlotDistributionStrategy.FIRST_COME_FIRST_SERVE) {
      slotDistributionWorkflows.handleNewJoinRequestFCFS(meetupEvent, persistedRequest);
    }
    eventPublisher.publishJoinRequestCreatedAfterTransaction(persistedRequest.getId());
  }

  public MeetupEvent confirmAttendee(UUID meetupId, MeetupJoinRequest joinRequest) {
    MeetupEvent meetupEvent = meetupDao.findOrThrow(meetupId);
    if (meetupEvent.getJoinRequests().stream()
        .filter(r -> r.getRequestState() == RequestState.OPEN)
        .noneMatch(r -> r.equals(joinRequest))) {
      throw new IllegalArgumentException(
          "Given join request is not persisted or state=open for meeting! " + joinRequest);
    }
    if (isFull(meetupEvent)) {
      throw new IllegalStateException(
          "Meeting is already full, can't confirm more requests: " + meetupEvent.logIdentity());
    }
    transitionToAccepted(joinRequest);
    eventPublisher.publishJoinRequestApprovedAfterTransaction(joinRequest.id());
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
    eventPublisher.publishJoinRequestCanceledAfterTransaction(request.getId());
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
    eventPublisher.publishMeetupCanceledAfterTransaction(meetup);
  }

  public void rescheduleEvent(UUID meetupId, ZonedDateTime newDate, ZonedDateTime newRegClose) {
    MeetupEvent meetup = meetupDao.findOrThrow(meetupId);
    log.info("Creator wants to reschedule meeting: {}", meetup.getId());
    meetup.setEventDate(newDate);
    meetup.setRegistrationClosing(newRegClose);
    meetupDao.createOrUpdate(meetup);
    eventPublisher.publishMeetupRescheduledAfterTransaction(meetup);
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

  public List<MeetupEvent> findPubliclyListed() {
    return meetupDao
        .findNotExpired(timeSource.now().atZone(siteManagementService.getServiceTimeZone()))
        .filter(m -> !m.isCanceled())
        .filter(meetupEvent -> meetupEvent.getVisibility() == MeetupVisibility.PUBLIC)
        .sorted(Comparator.comparing(MeetupEvent::getEventDate))
        .toList();
  }

  public List<MeetupEvent> findMeetupsByOrganizer(UUID currentUserId) {
    return meetupDao.findAllByOrganizer(currentUserId);
  }

  public void removeExpiredMeetup(UUID meetupId) {
    Optional<MeetupEvent> meetupEvent = meetupDao.find(meetupId);
    if (meetupEvent.isEmpty()) {
      log.info("Should remove meetup {} but couldn't find it in db - manually deleted?", meetupId);
      return;
    }
    if (meetupEvent.get().getEventDate().isBefore(timeSource.nowZDT())) {
      log.error("Given meetup {} is not expired!", meetupId);
      return;
    }
    log.info(
        "Deleting meetup '{}' because its eventdate is in the past, ID: {}",
        meetupEvent.get().getTitle(),
        meetupId);
    meetupDao.delete(meetupEvent.get());
  }
}

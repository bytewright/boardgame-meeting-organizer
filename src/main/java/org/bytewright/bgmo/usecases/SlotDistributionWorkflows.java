package org.bytewright.bgmo.usecases;

import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.domain.model.RequestState;
import org.bytewright.bgmo.domain.model.SlotDistributionStrategy;
import org.bytewright.bgmo.domain.model.event.ModelUpdatedEvents;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.bytewright.bgmo.domain.service.data.ModelDao;
import org.bytewright.bgmo.domain.service.event.EventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SlotDistributionWorkflows {
  private final ModelDao<MeetupJoinRequest> joinRequestDao;
  private final EventPublisher eventPublisher;
  private final MeetupDao meetupDao;

  @EventListener
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void onJoinRequestCreated(ModelUpdatedEvents.JoinRequestCreated event) {
    MeetupJoinRequest joinRequest = joinRequestDao.findOrThrow(event.id());
    if (joinRequest.getRequestState() != RequestState.OPEN) {
      return;
    }
    MeetupEvent meetupEvent = meetupDao.findOrThrow(joinRequest.getMeetupId());
    if (meetupEvent.getSlotStrategy() == SlotDistributionStrategy.FIRST_COME_FIRST_SERVE) {
      handleNewJoinRequestFCFS(meetupEvent, joinRequest);
    } else {
      log.debug("Event doesn't have auto-approve on: {}", meetupEvent.getId());
    }
  }

  @EventListener
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void onJoinRequestCanceled(ModelUpdatedEvents.JoinRequestCanceled event) {
    MeetupJoinRequest joinRequest = joinRequestDao.findOrThrow(event.id());
    MeetupEvent meetupEvent = meetupDao.findOrThrow(joinRequest.getMeetupId());
    if (meetupEvent.getSlotStrategy() == SlotDistributionStrategy.FIRST_COME_FIRST_SERVE) {
      handleJoinRequestCanceledFCFS(meetupEvent);
    } else {
      log.debug("Event doesn't have auto-approve on: {}", meetupEvent.getId());
    }
  }

  private void handleJoinRequestCanceledFCFS(MeetupEvent meetupEvent) {
    if (meetupEvent.isUnlimitedSlots()) {
      return;
    }
    List<MeetupJoinRequest> allRequests = meetupEvent.getJoinRequests();
    long approvedRequests =
        allRequests.stream().filter(r -> r.getRequestState() == RequestState.ACCEPTED).count();
    if (meetupEvent.getJoinSlots() <= approvedRequests) {
      return;
    }
    Optional<MeetupJoinRequest> firstFromWaitlist =
        allRequests.stream()
            .filter(r -> r.getRequestState() == RequestState.OPEN)
            .min(Comparator.comparing(MeetupJoinRequest::getTsCreation));
    if (firstFromWaitlist.isPresent()) {
      MeetupJoinRequest request = firstFromWaitlist.get();
      log.info("A slot for meetup became free, approving new request {}", request.id());
      approve(request);
    }
  }

  void handleNewJoinRequestFCFS(MeetupEvent meetupEvent, MeetupJoinRequest joinRequest) {
    if (meetupEvent.isUnlimitedSlots()) {
      approve(joinRequest);
      return;
    }
    List<MeetupJoinRequest> allRequests = meetupEvent.getJoinRequests();
    long approvedRequests =
        allRequests.stream().filter(r -> r.getRequestState() == RequestState.ACCEPTED).count();
    if (meetupEvent.getJoinSlots() <= approvedRequests) {
      eventPublisher.publishJoinRequestWaitlistedAfterTransaction(joinRequest.getId());
      return;
    }
    approve(joinRequest);
  }

  private void approve(MeetupJoinRequest joinRequest) {
    log.info("Auto-approving join request: {}", joinRequest.getId());
    joinRequest.setRequestState(RequestState.ACCEPTED);
    joinRequestDao.createOrUpdate(joinRequest);
    eventPublisher.publishJoinRequestApprovedAfterTransaction(joinRequest.getId());
  }
}

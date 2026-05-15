package org.bytewright.bgmo.domain.service.event;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.event.ModelUpdatedEvents;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {
  private final ApplicationEventPublisher applicationEventPublisher;

  public void publishMeetupCreatedAfterTransaction(MeetupEvent event) {
    var meetupCreatedEvent = new ModelUpdatedEvents.MeetupCreated(event.getId());
    publishAfterTransaction(meetupCreatedEvent);
  }

  public void publishMeetupRescheduledAfterTransaction(MeetupEvent event) {
    var meetupCreatedEvent = new ModelUpdatedEvents.MeetupRescheduled(event.getId());
    publishAfterTransaction(meetupCreatedEvent);
  }

  public void publishMeetupCanceledAfterTransaction(MeetupEvent event) {
    var meetupCreatedEvent = new ModelUpdatedEvents.MeetupCanceled(event.getId());
    publishAfterTransaction(meetupCreatedEvent);
  }

  public void publishJoinRequestCreatedAfterTransaction(UUID requestId) {
    var event = new ModelUpdatedEvents.JoinRequestCreated(requestId);
    publishAfterTransaction(event);
  }

  public void publishJoinRequestApprovedAfterTransaction(UUID requestId) {
    var event = new ModelUpdatedEvents.JoinRequestApproved(requestId);
    publishAfterTransaction(event);
  }

  public void publishJoinRequestCanceledAfterTransaction(UUID requestId) {
    var event = new ModelUpdatedEvents.JoinRequestCanceled(requestId);
    publishAfterTransaction(event);
  }

  public void publishJoinRequestWaitlistedAfterTransaction(UUID requestId) {
    var event = new ModelUpdatedEvents.JoinRequestWaitlisted(requestId);
    publishAfterTransaction(event);
  }

  public void publishUserCreatedAfterTransaction(UUID userId) {
    var event = new ModelUpdatedEvents.UserCreated(userId);
    publishAfterTransaction(event);
  }

  public void publishUserVerifiedAfterTransaction(UUID userId) {
    var event = new ModelUpdatedEvents.UserVerified(userId);
    publishAfterTransaction(event);
  }

  private void publishAfterTransaction(ModelUpdatedEvents event) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            log.info("Publishing {}", event);
            applicationEventPublisher.publishEvent(event);
          }
        });
  }
}

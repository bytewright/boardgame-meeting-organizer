package org.bytewright.bgmo.domain.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.event.MeetupCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {
  private final ApplicationEventPublisher applicationEventPublisher;

  public void publishAfterTransaction(MeetupEvent event) {
    MeetupCreatedEvent meetupCreatedEvent = new MeetupCreatedEvent(event.getId());
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            log.info("Publishing {}", meetupCreatedEvent);
            applicationEventPublisher.publishEvent(meetupCreatedEvent);
          }
        });
  }
}

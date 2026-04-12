package org.bytewright.bgmo.usecases;

import jakarta.transaction.Transactional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.MeetupCreation;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.automation.TimeSource;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MeetupWorkflows {
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
            .durationHours(event.getDurationHours())
            .build();
    return meetupDao.createOrUpdate(meetupEvent);
  }

  public void requestToJoin(UUID meetupId, UUID userId, String comment) {
    // todo comment will be added later to request
    MeetupEvent meetupEvent = meetupDao.findOrThrow(meetupId);
    if (meetupEvent.getJoinRequests().stream().anyMatch(r -> r.getUserId().equals(userId))) {
      log.info("User {} tried to join meeting he already has requested to join...", userId);
      return;
    }
    RegisteredUser user = userDao.findOrThrow(userId);
    var request = new MeetupJoinRequest(meetupEvent.getId(), user.getId(), timeSource.now());
    meetupEvent.getJoinRequests().add(request);
    meetupDao.createOrUpdate(meetupEvent);
  }

  public MeetupEvent confirmAttendees(UUID meetupId, Set<UUID> attendeeIds) {
    MeetupEvent meetupEvent = meetupDao.findOrThrow(meetupId);
    Set<UUID> confirmedAttendeeIds =
        Stream.concat(meetupEvent.getConfirmedAttendeeIds().stream(), attendeeIds.stream())
            .collect(Collectors.toSet());
    if (!meetupEvent.isUnlimitedSlots()
        && confirmedAttendeeIds.size() > meetupEvent.getJoinSlots()) {
      throw new IllegalArgumentException(
          "Attendee count %d would be too much for event %s"
              .formatted(confirmedAttendeeIds.size(), meetupId));
    }
    meetupEvent.getConfirmedAttendeeIds().addAll(confirmedAttendeeIds);
    return meetupDao.createOrUpdate(meetupEvent);
  }
}

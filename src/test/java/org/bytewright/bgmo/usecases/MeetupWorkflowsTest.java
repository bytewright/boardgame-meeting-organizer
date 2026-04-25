package org.bytewright.bgmo.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.bytewright.bgmo.domain.model.*;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.bytewright.bgmo.testutils.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MeetupWorkflowsTest extends IntegrationTest {
  @Autowired private MeetupWorkflows meetupWorkflows;
  @Autowired private UserWorkflows userWorkflows;
  @Autowired private MeetupDao meetupDao;

  @Test
  void testCreateAndJoinMeetup() {
    // ARRANGE
    RegisteredUser creator = helper.user();
    Game game = Game.builder().name("Testgame").build();
    UUID gameId = userWorkflows.addGameToLibrary(creator.getId(), game).getId();
    RegisteredUser joiner = helper.user();
    MeetupEvent meetupEvent =
        meetupWorkflows.create(
            MeetupCreation.builder()
                .title("myEvent")
                .creator(creator)
                .eventDate(helper.now())
                .registrationClosingDate(helper.now())
                .joinSlots(1)
                .offeredGames(List.of(gameId))
                .build());

    // ACT
    meetupWorkflows.requestToJoin(meetupEvent.getId(), joiner.getId(), null);

    // ASSERT
    MeetupEvent meetup = meetupDao.findById(meetupEvent.getId()).orElseThrow();
    assertThat(meetup.getJoinRequests()).hasSize(1);
    MeetupJoinRequest request = meetup.getJoinRequests().getFirst();
    assertThat(request)
        .returns(meetup.getId(), MeetupJoinRequest::getMeetupId)
        .returns(joiner.getId(), MeetupJoinRequest::getUserId)
        .returns(RequestState.OPEN, MeetupJoinRequest::getRequestState);

    // ACT
    MeetupEvent refetchedEvent = meetupWorkflows.confirmAttendee(meetup.getId(), request);

    // ASSERT
    MeetupJoinRequest refetchedRequest = refetchedEvent.getJoinRequests().getFirst();
    assertThat(refetchedRequest)
        .returns(meetup.getId(), MeetupJoinRequest::getMeetupId)
        .returns(joiner.getId(), MeetupJoinRequest::getUserId)
        .returns(RequestState.ACCEPTED, MeetupJoinRequest::getRequestState);
  }
}

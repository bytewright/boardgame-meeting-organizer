package org.bytewright.bgmo.usecases;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.Game;
import org.bytewright.bgmo.domain.model.MeetupCreation;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.automation.TimeSource;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class MockDataInitializer implements ApplicationListener<ApplicationReadyEvent> {
  private static final int NUM_TEST_ADMINS = 1;
  private static final int NUM_TEST_USERS = 10;
  private final UserWorkflows userWorkflows;
  private final MeetupWorkflows meetupWorkflows;
  private final TimeSource timeSource;

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    List<RegisteredUser> admins = new ArrayList<>();
    List<RegisteredUser> users = new ArrayList<>();
    List<MeetupEvent> meetupEvents = new ArrayList<>();
    UUID adminMeetup = null;
    for (int i = 0; i < NUM_TEST_ADMINS; i++) {
      RegisteredUser.Creation user = createAdmin(i);
      RegisteredUser registeredUser = userWorkflows.create(user);
      userWorkflows.addGameToLibrary(
          registeredUser.getId(),
          Game.builder().name("Super Game-" + i).minPlayers(1).maxPlayers(4).build());
      admins.add(registeredUser);
      MeetupCreation creation =
          MeetupCreation.builder()
              .creator(registeredUser)
              .title("Test event-" + i)
              .description("For testing purposes")
              .durationHours(2)
              .eventDate(timeSource.nowZDT().plus(Duration.ofHours(10)))
              .build();
      meetupWorkflows.create(
          creation.toBuilder().eventDate(timeSource.nowZDT().minusHours(1)).build());
      MeetupEvent meetupEvent = meetupWorkflows.create(creation);
      adminMeetup = meetupEvent.getId();
    }
    for (int i = 0; i < NUM_TEST_USERS; i++) {
      RegisteredUser.Creation user = createUser(i);
      log.info("User: {} pw: {}", user.getLoginName(), user.getPassword());
      RegisteredUser registeredUser = userWorkflows.create(user);
      Game game =
          userWorkflows.addGameToLibrary(
              registeredUser.getId(),
              Game.builder().name("Mega Game-" + i).minPlayers(1).maxPlayers(4).build());
      users.add(registeredUser);
      MeetupCreation creation =
          MeetupCreation.builder()
              .creator(registeredUser)
              .title("Test event-" + i)
              .description("For testing purposes")
              .durationHours(2)
              .eventDate(timeSource.nowZDT().plus(Duration.ofDays(1 + i * 2)))
              .offeredGames(List.of(game.id()))
              .build();
      meetupWorkflows.create(
          creation.toBuilder().eventDate(timeSource.nowZDT().minusHours(1)).build());
      MeetupEvent meetupEvent = meetupWorkflows.create(creation);
      meetupEvents.add(meetupEvent);

      meetupWorkflows.requestToJoin(adminMeetup, registeredUser.getId(), null);
    }
    MeetupEvent meetupEvent = meetupEvents.getFirst();
    meetupWorkflows.requestToJoin(meetupEvent.getId(), admins.getFirst().getId(), null);
    meetupWorkflows.requestToJoinAnon(meetupEvent.getId(), UUID.randomUUID(), "Anon", "noWhere");
    log.warn("Added {} users and {} admins: {}", NUM_TEST_USERS, NUM_TEST_ADMINS, admins);
  }

  private RegisteredUser.Creation createUser(int index) {
    String userName = "user-%d".formatted(index);
    return RegisteredUser.Creation.builder()
        .loginName(userName)
        .displayName(userName)
        .password("admin")
        .email("%s@some.mail".formatted(userName))
        .build();
  }

  private RegisteredUser.Creation createAdmin(int index) {
    String userName = index == 0 ? "admin" : "admin-%d".formatted(index);
    return RegisteredUser.Creation.builder()
        .loginName(userName)
        .displayName(userName)
        .password("admin")
        .email("%s@admin.mail".formatted(userName))
        .build();
  }
}

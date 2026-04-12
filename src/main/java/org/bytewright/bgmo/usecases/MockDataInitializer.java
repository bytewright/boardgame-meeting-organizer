package org.bytewright.bgmo.usecases;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  private static final int NUM_TEST_USERS = 2;
  private final UserWorkflows userWorkflows;
  private final MeetupWorkflows meetupWorkflows;
  private final TimeSource timeSource;

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    List<RegisteredUser> admins = new ArrayList<>();
    List<RegisteredUser> users = new ArrayList<>();

    for (int i = 0; i < NUM_TEST_ADMINS; i++) {
      RegisteredUser user = createAdmin(i);
      userWorkflows.create(user).ifPresent(admins::add);
    }
    for (int i = 0; i < NUM_TEST_USERS; i++) {
      RegisteredUser user = createUser(i);
      log.info("User: {} pw: {}", user.getEmail(), user.getPasswordHash());
      userWorkflows.create(user).ifPresent(users::add);
    }
    MeetupCreation creation =
        MeetupCreation.builder()
            .creator(users.getFirst())
            .title("Test event")
            .description("For testing purposes")
            .durationHours(2)
            .eventDate(timeSource.nowZDT().plus(Duration.ofDays(7)))
            .build();
    meetupWorkflows.create(
        creation.toBuilder().eventDate(timeSource.nowZDT().minusHours(1)).build());
    MeetupEvent meetupEvent = meetupWorkflows.create(creation);
    meetupWorkflows.requestToJoin(meetupEvent.getId(), admins.getFirst().getId(), null);
    log.warn("Added {} users and {} admins: {}", NUM_TEST_USERS, NUM_TEST_ADMINS, admins);
  }

  private RegisteredUser createUser(int index) {
    String userName = "user-%d".formatted(index);
    return RegisteredUser.builder()
        .name(userName)
        .passwordHash("noPw")
        .email("%s@some.mail".formatted(userName))
        .build();
  }

  private RegisteredUser createAdmin(int index) {
    String userName = index == 0 ? "admin" : "admin-%d".formatted(index);
    return RegisteredUser.builder()
        .name(userName)
        .passwordHash("admin")
        .email("%s@admin.mail".formatted(userName))
        .build();
  }
}

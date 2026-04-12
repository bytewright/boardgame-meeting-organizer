package org.bytewright.bgmo.usecases;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
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

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    List<RegisteredUser> admins = new ArrayList<>();

    for (int i = 0; i < NUM_TEST_ADMINS; i++) {
      RegisteredUser user = createAdmin(i);
      userWorkflows.create(user).ifPresent(admins::add);
    }
    for (int i = 0; i < NUM_TEST_USERS; i++) {
      RegisteredUser user = createUser(i);
      userWorkflows.create(user);
    }

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

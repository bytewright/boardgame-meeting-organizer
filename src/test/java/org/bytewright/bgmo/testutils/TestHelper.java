package org.bytewright.bgmo.testutils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.automation.TimeSource;
import org.bytewright.bgmo.usecases.UserWorkflows;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TestHelper {
  private static final Random rnd = new SecureRandom("TestSeed".getBytes(StandardCharsets.UTF_8));
  private final UserWorkflows userWorkflows;
  private final TimeSource timeSource;

  public RegisteredUser user() {
    String uniqueName = "rndName%s".formatted(rnd.nextInt());
    return userWorkflows
        .create(
            RegisteredUser.builder()
                .loginName(uniqueName)
                .displayName(uniqueName)
                .email("%s@mail.com".formatted(uniqueName))
                .passwordHash("somePwHash")
                .build())
        .orElseThrow();
  }

  public ZonedDateTime now() {
    return ZonedDateTime.now(timeSource.clock());
  }
}

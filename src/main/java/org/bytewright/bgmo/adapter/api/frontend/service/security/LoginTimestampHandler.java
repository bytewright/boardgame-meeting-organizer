package org.bytewright.bgmo.adapter.api.frontend.service.security;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bytewright.bgmo.usecases.UserWorkflows;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginTimestampHandler implements ApplicationListener<AuthenticationSuccessEvent> {

  private final UserWorkflows userWorkflows; // use case, not DAO directly

  @Override
  public void onApplicationEvent(AuthenticationSuccessEvent event) {
    UUID userId = UUID.fromString(event.getAuthentication().getName());
    userWorkflows.recordLogin(userId);
  }
}

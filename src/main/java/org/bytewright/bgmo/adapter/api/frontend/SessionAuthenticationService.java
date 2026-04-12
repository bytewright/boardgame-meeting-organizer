package org.bytewright.bgmo.adapter.api.frontend;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.user.AuthenticationService;
import org.bytewright.bgmo.domain.service.user.CurrentUserAccessor;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@VaadinSessionScope
@RequiredArgsConstructor
public class SessionAuthenticationService implements CurrentUserAccessor {

  private static final String CURRENT_USER_SESSION_KEY = "currentUserId";
  private final AuthenticationService authenticationService;

  public boolean login(String email, String password) {
    Optional<RegisteredUser> userOptional = authenticationService.login(email, password);
    if (userOptional.isPresent()) {
      UUID userId = userOptional.map(RegisteredUser::getId).orElseThrow();
      log.info("User has logged in using password: {}", userId);
      // Store user in Vaadin session
      VaadinSession.getCurrent().setAttribute(CURRENT_USER_SESSION_KEY, userId);
      return true;
    }
    return false;
  }

  public void logout() {
    try {
      getCurrentUser().map(RegisteredUser::getId).ifPresent(authenticationService::logout);
    } finally {
      VaadinSession.getCurrent().setAttribute(CURRENT_USER_SESSION_KEY, null);
      // I think thats not needed?
      // VaadinSession.getCurrent().close();
    }
  }

  @Override
  public Optional<RegisteredUser> getCurrentUser() {
    UUID userId = (UUID) VaadinSession.getCurrent().getAttribute(CURRENT_USER_SESSION_KEY);
    return Optional.ofNullable(userId).flatMap(authenticationService::findById);
  }

  public void passwordReset() {
    log.info("User has forgot the password, todo add impl.");
  }
}

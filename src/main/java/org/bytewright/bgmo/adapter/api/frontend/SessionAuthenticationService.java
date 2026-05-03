package org.bytewright.bgmo.adapter.api.frontend;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.model.user.UserRole;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.domain.service.user.CurrentUserAccessor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionAuthenticationService implements CurrentUserAccessor {
  private final RegisteredUserDao registeredUserDao;

  @Override
  public Optional<RegisteredUser> getCurrentUser() {
    return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
        .filter(Authentication::isAuthenticated)
        .filter(auth -> !(auth instanceof AnonymousAuthenticationToken))
        .map(Authentication::getName) // this is the UUID string, set by app impl of
        // org.springframework.security.core.userdetails.UserDetailsService
        .map(UUID::fromString)
        .flatMap(registeredUserDao::find);
  }

  public void logout() {
    // bgmoVaadinWebSecurity.getAuthenticationContext().logout();
    // SecurityContextHolder is cleared automatically
    VaadinSession.getCurrent().getSession().invalidate();
    UI.getCurrent().getPage().setLocation("/dashboard");
  }

  public void passwordReset() {
    log.info("Password reset requested — not yet implemented.");
  }

  public boolean isCurrentUserAdmin() {
    return getCurrentUser()
        .map(registeredUser -> registeredUser.getRole() == UserRole.ADMIN)
        .orElse(false);
  }
}

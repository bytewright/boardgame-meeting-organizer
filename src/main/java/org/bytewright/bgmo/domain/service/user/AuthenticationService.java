package org.bytewright.bgmo.domain.service.user;

import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthenticationService {

  private final RegisteredUserDao registeredUserModelDao;

  public Optional<RegisteredUser> login(String email, String password) {
    // TODO integrate in spring security
    log.info("User is attempting to log in: {}", email);
    Optional<RegisteredUser> userOptional = registeredUserModelDao.findBy(email, password);
    if (userOptional.isPresent()) {
      RegisteredUser user = userOptional.get();
      user.setTsLastLogin(Instant.now());
      return Optional.of(registeredUserModelDao.createOrUpdate(user));
    }
    return Optional.empty();
  }

  public void logout(UUID userId) {
    log.info("User is log out: {}", userId);
  }

  public Optional<RegisteredUser> findById(UUID userId) {
    return registeredUserModelDao.find(userId);
  }
}

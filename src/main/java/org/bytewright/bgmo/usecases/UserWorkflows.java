package org.bytewright.bgmo.usecases;

import jakarta.transaction.Transactional;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserWorkflows {
  private final RegisteredUserDao userDao;

  public Optional<RegisteredUser> create(RegisteredUser user) {
    if (user.getId() != null) {
      throw new IllegalArgumentException("User has an id already");
    }
    return Optional.of(userDao.createOrUpdate(user));
  }
}

package org.bytewright.bgmo.domain.service.security;

import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BgmoUserDetailsService implements UserDetailsService, UserDetailsPasswordService {
  private final PepperingPasswordEncoder passwordEncoder;
  private final RegisteredUserDao userDao;

  @Override
  public UserDetails loadUserByUsername(String loginName) throws UsernameNotFoundException {
    return userDao
        .findByLoginName(loginName)
        .map(this::userDetailsFromRegisteredUserEntity)
        .orElseThrow(() -> new UsernameNotFoundException(loginName));
  }

  private UserDetails userDetailsFromRegisteredUserEntity(RegisteredUser user) {
    return User.builder()
        .accountLocked(user.getStatus().isLocked())
        .username(user.getId().toString()) // UUID as principal name
        .password(user.getPasswordHash())
        .roles(user.getRole().name())
        .build();
  }

  public void updatePasswordEncodeFirstAndPersist(RegisteredUser user, String newPassword) {
    if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
      log.debug("Update password was called but given PW for user matches old pw, skipping...");
      return;
    }
    String encodedPw = hashPw(newPassword);
    updatePasswordAndPersist(user, encodedPw);
  }

  public RegisteredUser updatePasswordAndPersist(RegisteredUser user, String newPassword) {
    log.info("User {} changed his password", user.logEntity());
    user.setPasswordHash(newPassword);
    return userDao.createOrUpdate(user);
  }

  public String hashPw(String password) {
    return passwordEncoder.encode(password);
  }

  @Override
  public UserDetails updatePassword(UserDetails user, String newPassword) {
    RegisteredUser registeredUser = toRegisteredUser(user);
    log.info("Updating PW for user {}", registeredUser.logEntity());
    return userDetailsFromRegisteredUserEntity(
        updatePasswordAndPersist(registeredUser, newPassword));
  }

  private RegisteredUser toRegisteredUser(UserDetails user) {
    return userDao.findOrThrow(UUID.fromString(user.getUsername()));
  }
}

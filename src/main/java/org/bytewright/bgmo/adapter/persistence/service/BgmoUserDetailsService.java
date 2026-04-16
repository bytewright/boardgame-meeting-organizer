package org.bytewright.bgmo.adapter.persistence.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.persistence.dao.repository.RegisteredUserRepository;
import org.bytewright.bgmo.adapter.persistence.entity.user.RegisteredUserEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BgmoUserDetailsService implements UserDetailsService {
  private final RegisteredUserRepository repository;

  @Override
  public UserDetails loadUserByUsername(String loginName) throws UsernameNotFoundException {
    return repository
        .findByLoginName(loginName)
        .map(this::userDetailsFromRegisteredUserEntity)
        .orElseThrow(() -> new UsernameNotFoundException(loginName));
  }

  private UserDetails userDetailsFromRegisteredUserEntity(RegisteredUserEntity entity) {
    UserDetails userDetails =
        User.builder()
            .accountLocked(entity.getStatus().isLocked())
            .username(entity.getId().toString()) // UUID as principal name
            .password(entity.getPasswordHash())
            .roles(entity.getRole().name())
            .build();
    log.info("Converted user: {}", userDetails);
    return userDetails;
  }
}

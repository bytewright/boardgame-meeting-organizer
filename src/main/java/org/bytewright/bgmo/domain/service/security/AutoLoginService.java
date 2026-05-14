package org.bytewright.bgmo.domain.service.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoLoginService {
  private final AuthenticationManager authenticationManager;

  public void loginAfterRegister(RegisteredUser user, String plainTextPw) {
    log.info("Attempting autologin for new user {}", user.getId());
    var token = new UsernamePasswordAuthenticationToken(user.getLoginName(), plainTextPw);
    Authentication auth = authenticationManager.authenticate(token);
    SecurityContextHolder.getContext().setAuthentication(auth);
  }
}

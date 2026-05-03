package org.bytewright.bgmo.domain.service.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * What if the pepper leaks? System degrades to normal hashing (like argon2 without pepper)
 *
 * <p>Pepper is defense-in-depth, not the only protection.
 *
 * <p>Rotate it like a secret if compromised, rotation is hard → requires password reset campaign
 */
@Slf4j
public class PepperingPasswordEncoder implements PasswordEncoder {

  private final PasswordEncoder delegate;
  private final String pepper;

  public PepperingPasswordEncoder(PasswordEncoder delegate, String pepper) {
    this.delegate = delegate;
    this.pepper = pepper;
  }

  @Override
  public String encode(CharSequence rawPassword) {
    return delegate.encode(rawPassword + pepper);
  }

  @Override
  public boolean matches(CharSequence rawPassword, String encodedPassword) {
    return delegate.matches(rawPassword + pepper, encodedPassword);
  }

  @Override
  public boolean upgradeEncoding(String encodedPassword) {
    log.debug("Upgrading encoding of user pw!");
    return delegate.upgradeEncoding(encodedPassword);
  }
}

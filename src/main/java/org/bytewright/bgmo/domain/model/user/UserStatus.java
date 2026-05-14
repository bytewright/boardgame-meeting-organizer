package org.bytewright.bgmo.domain.model.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus {
  PENDING_APPROVAL(true),
  AFTER_REGISTRATION(false),
  ACTIVE(false),
  SUSPENDED(true),
  BANNED(true),
  DELETED(true);
  private final boolean locked;
}

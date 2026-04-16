package org.bytewright.bgmo.domain.model.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus {
  PENDING_APPROVAL(true),
  ACTIVE(false),
  BANNED(true);
  private final boolean locked;
}

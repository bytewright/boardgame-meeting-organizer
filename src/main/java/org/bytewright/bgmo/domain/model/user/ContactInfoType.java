package org.bytewright.bgmo.domain.model.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContactInfoType {
  EMAIL(true),
  TELEGRAM(true),
  SIGNAL(true),
  ADDRESS(false),
  PHONE(true);
  private final boolean canBePrimary;
}

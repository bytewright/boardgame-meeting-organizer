package org.bytewright.bgmo.domain.model.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContactInfoType {
  EMAIL(true, "contact.type.email.name"),
  TELEGRAM(true, "contact.type.telegram.name"),
  SIGNAL(true, "contact.type.signal.name"),
  ADDRESS(false, "contact.type.address.name"),
  PHONE(true, "contact.type.phone.name");
  private final boolean canBePrimary;
  private final String nameMessageKey;
}

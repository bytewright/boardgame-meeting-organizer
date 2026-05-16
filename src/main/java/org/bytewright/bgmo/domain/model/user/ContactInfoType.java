package org.bytewright.bgmo.domain.model.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContactInfoType {
  EMAIL(true, false, "contact.type.email.name"),
  TELEGRAM(true, true, "contact.type.telegram.name"),
  SIGNAL(true, true, "contact.type.signal.name"),
  ADDRESS(false, false, "contact.type.address.name"),
  PHONE(true, false, "contact.type.phone.name");
  private final boolean canBePrimary;
  private final boolean singletonContact;
  private final String nameMessageKey;
}

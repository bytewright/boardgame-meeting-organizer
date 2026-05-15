package org.bytewright.bgmo.domain.model.user.exception;

import org.bytewright.bgmo.domain.model.exception.ExceptionWithMessageKey;

public class ModifyContactsException extends ExceptionWithMessageKey {
  public ModifyContactsException(String msg) {
    super(msg);
  }

  public static ModifyContactsException lastContact() {
    return new ModifyContactsException("user.contacts.error.remove-last");
  }
}

package org.bytewright.bgmo.domain.model.notification;

import org.bytewright.bgmo.domain.model.user.ContactOption;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;

public sealed interface VerificationAttempt
    permits VerificationAttempt.Success, VerificationAttempt.Failed {
  record Success(RegisteredUser user, ContactOption contactOption) implements VerificationAttempt {}

  record Failed() implements VerificationAttempt {}
}

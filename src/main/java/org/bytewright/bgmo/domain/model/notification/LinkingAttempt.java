package org.bytewright.bgmo.domain.model.notification;

import java.util.UUID;

public sealed interface LinkingAttempt
    permits LinkingAttempt.Failed, LinkingAttempt.LinkAndContactOption, LinkingAttempt.Success {

  record Success(UUID userId) implements LinkingAttempt {}

  record LinkAndContactOption(UUID userId, UUID contactOptionId) implements LinkingAttempt {}

  record Failed() implements LinkingAttempt {}
}

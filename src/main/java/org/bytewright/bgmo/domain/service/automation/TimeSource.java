package org.bytewright.bgmo.domain.service.automation;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;

public interface TimeSource {
  Instant now();

  Clock clock();

  default ZonedDateTime nowZDT() {
    return ZonedDateTime.now(clock());
  }
}

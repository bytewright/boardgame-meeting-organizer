package org.bytewright.bgmo.domain.service.automation;

import java.time.Clock;
import java.time.Instant;

public interface TimeSource {
  Instant now();

  Clock clock();
}

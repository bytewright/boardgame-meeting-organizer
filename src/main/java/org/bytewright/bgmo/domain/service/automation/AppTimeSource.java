package org.bytewright.bgmo.domain.service.automation;

import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppTimeSource implements TimeSource {
  private final Clock clock;

  public Instant now() {
    return Instant.now(clock);
  }

  @Override
  public Clock clock() {
    return clock;
  }
}

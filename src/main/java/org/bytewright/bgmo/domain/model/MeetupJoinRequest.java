package org.bytewright.bgmo.domain.model;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MeetupJoinRequest {
  private UUID meetupId;
  private UUID userId;
  private Instant tsCreation;
}

package org.bytewright.bgmo.domain.model;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class MeetupJoinRequest {
  private UUID meetupId;
  private UUID userId;

  /**
   * The name the requester chose to be shown publicly (once confirmed). Required for both
   * registered and anonymous requesters; for registered users this is pre-filled with {@code
   * RegisteredUser#getName()}.
   */
  private String displayName;

  private Instant tsCreation;
}

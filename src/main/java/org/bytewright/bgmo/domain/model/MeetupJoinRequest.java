package org.bytewright.bgmo.domain.model;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import org.bytewright.bgmo.domain.model.data.HasUUID;

/** Represents a request to join a meetup, from either a registered or anonymous user. */
@Data
@Builder
public class MeetupJoinRequest implements HasUUID {
  private UUID id;
  private UUID meetupId;
  private Instant tsCreation;
  @Nullable private String comment;
  @Builder.Default private RequestState requestState = RequestState.OPEN;
  private JoinRequestPayload payload;
}

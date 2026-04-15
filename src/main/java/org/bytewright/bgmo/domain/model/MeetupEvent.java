package org.bytewright.bgmo.domain.model;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import org.bytewright.bgmo.domain.model.data.HasUUID;

/**
 * @see MeetupCreation for creation dto
 */
@Data
@Builder
public class MeetupEvent implements HasUUID {
  private UUID id;
  private Instant tsCreation;
  private Instant tsModified;
  private String title;
  private String description;
  // todo game links & game poll
  // todo rescheduled events/choose-able start dates
  private ZonedDateTime eventDate;
  private int durationHours;
  private int joinSlots;
  private boolean unlimitedSlots;
  private boolean allowAnonSignup;
  private boolean canceled;
  private UUID creatorId;
  private List<MeetupJoinRequest> joinRequests;
  private List<UUID> offeredGames;

  public String logIdentity() {
    return "Meetup['%s';%s]".formatted(title, id);
  }
}

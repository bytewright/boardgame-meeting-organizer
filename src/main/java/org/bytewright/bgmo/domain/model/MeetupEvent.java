package org.bytewright.bgmo.domain.model;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import org.bytewright.bgmo.domain.model.data.HasUUID;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;

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
  private ZonedDateTime eventDate;
  private ZonedDateTime registrationClosing;
  private MeetupVisibility visibility;
  private SlotDistributionStrategy slotStrategy;
  private int durationHours;
  private int joinSlots;
  private boolean unlimitedSlots;
  private boolean allowAnonSignup;
  private boolean canceled;
  private UUID creatorId;
  private String areaHint;
  private String fullLocation;
  private List<MeetupJoinRequest> joinRequests;
  private List<UUID> offeredGames;

  public String logIdentity() {
    return "Meetup['%s';%s]".formatted(title, id);
  }

  @Data
  @Builder(toBuilder = true)
  public static class MeetupCreation {
    private String title;
    private String description;
    private ZonedDateTime eventDate;
    private LocalDate registrationClosingDate;
    private int durationHours;
    @Nullable private Integer joinSlots;
    private boolean unlimitedSlots;
    private boolean allowAnonSignup;
    private RegisteredUser creator;
    private SlotDistributionStrategy slotStrategy;
    @Builder.Default private MeetupVisibility visibility = MeetupVisibility.PUBLIC;
    private MeetupEventLocation location;
    private List<UUID> offeredGames;
  }
}

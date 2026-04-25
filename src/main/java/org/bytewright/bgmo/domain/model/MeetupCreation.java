package org.bytewright.bgmo.domain.model;

import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;

/**
 * This is a minimal event creation DTO
 *
 * @see MeetupEvent
 */
@Data
@Builder(toBuilder = true)
public class MeetupCreation {
  private String title;
  private String description;
  private ZonedDateTime eventDate;
  private ZonedDateTime registrationClosingDate;
  private int durationHours;
  @Nullable private Integer joinSlots;
  private boolean unlimitedSlots;
  private boolean allowAnonSignup;
  private RegisteredUser creator;
  private List<UUID> offeredGames;
}

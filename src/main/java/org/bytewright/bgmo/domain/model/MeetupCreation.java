package org.bytewright.bgmo.domain.model;

import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Data;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;

/**
 * @see MeetupEvent
 */
@Data
@Builder(toBuilder = true)
public class MeetupCreation {
  private String title;
  private String description;
  private ZonedDateTime eventDate;
  private int durationHours;
  @Nullable private Integer joinSlots;
  private boolean unlimitedSlots;
  private RegisteredUser creator;
}

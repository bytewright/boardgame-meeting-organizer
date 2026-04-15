package org.bytewright.bgmo.domain.model;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import org.bytewright.bgmo.domain.model.data.HasUUID;

@Data
@Builder(toBuilder = true)
public class Game implements HasUUID {
  private UUID id;
  private UUID ownerId;

  private String name;
  @Nullable private String description;
  private int minPlayers;
  private int maxPlayers;
  @Nullable private Integer optimalPlayers;

  /** Provided by the owner of the game, a rough estimation */
  @Nullable private Integer playTimeMinutesPerPlayer;

  /** BoardGameGeek id */
  @Nullable private Long bggId; // Numeric BGG ID for API usage

  /** copy from BGG complexity, a measure of how serious the game meetup is going to be */
  @Nullable private Double complexity; // BGG Weight rating (e.g., 1.0 to 5.0)

  @Nullable private String artworkLink;

  /** User defined, can point to anything - house rules, googlemaps, errata, YouTube tutorial,... */
  private List<String> urls;
  // ToDo ideas for the future: playstats/play count
}

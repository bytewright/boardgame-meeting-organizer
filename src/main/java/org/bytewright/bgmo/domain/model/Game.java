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
  @Nullable private Double complexity; // Weight rating (e.g., 1.0 to 5.0)
  private int minPlayers;
  private int maxPlayers;
  @Nullable private Integer optimalPlayers;
  @Nullable private Integer playTimeMinutesPerPlayer;
  @Nullable private Long bggId; // Numeric BGG ID for API usage
  private List<String> urls;
}

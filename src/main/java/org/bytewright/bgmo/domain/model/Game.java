package org.bytewright.bgmo.domain.model;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.ToString;
import org.bytewright.bgmo.domain.model.data.HasUUID;

@Data
@Builder(toBuilder = true)
public class Game implements HasUUID {
  private UUID id;
  private UUID ownerId;
  private Instant tsCreation;
  private Instant tsModified;
  @Nullable private Instant tsDeleted;
  private String name;
  @ToString.Exclude @Nullable private String description;

  /** free-text notes, "I own all expansions" or house-rule reminders. */
  @ToString.Exclude @Nullable private String notes;

  private int minPlayers;
  private int maxPlayers;
  @Nullable private Integer optimalPlayers;

  /** Provided by the owner of the game, a rough estimation */
  @Nullable private Integer playTimeMinutesPerPlayer;

  /** BoardGameGeek id */
  @Nullable private Long bggId;

  /**
   * copy from BGG complexity, a measure of how serious the game meetup is going to be.<br>
   * BGG Weight rating (e.g., 1.0 to 5.0)
   */
  @Nullable private Double complexity;

  @Nullable private String artworkLink;

  /** Short descriptors like "worker placement", "cooperative", "filler". */
  @Builder.Default private List<String> tags = new ArrayList<>();

  /** User defined links. Can point to house rules, errata, YouTube tutorials, etc. */
  @ToString.Exclude @Builder.Default private List<UserLink> urls = new ArrayList<>();

  /**
   * A URL together with an optional human-readable display text.
   *
   * @param url the target URL (required)
   * @param displayText anchor label shown in the UI; falls back to the raw URL when blank/null
   */
  public record UserLink(String url, String displayText) {}

  @Data
  @Builder(toBuilder = true)
  public static class Creation {
    private String name;
    private int minPlayers;
    private int maxPlayers;
    @ToString.Exclude @Nullable private String description;
    @ToString.Exclude @Nullable private String notes;
    @Nullable private Integer optimalPlayers;
    @Nullable private Integer playTimeMinutesPerPlayer;
    @Nullable private Long bggId;
    @Nullable private Double complexity;
    @Nullable private String artworkLink;
    @ToString.Exclude @Singular private List<UserLink> urls;
    @ToString.Exclude @Singular private List<String> tags;
  }
}

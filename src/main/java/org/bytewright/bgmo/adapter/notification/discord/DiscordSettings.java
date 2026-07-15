package org.bytewright.bgmo.adapter.notification.discord;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class DiscordSettings {

  @Builder.Default private boolean enabled = false;
  @Builder.Default private String tutorialStep2Link = "Some first step";
  @Builder.Default private String tutorialStep3Link = "Some second step";
  @Builder.Default private List<AnnouncementChannel> announcementChannels = new ArrayList<>();

  /** Represents a Discord channel where announcements should be sent. */
  @Data
  @Builder
  @Jacksonized
  public static class AnnouncementChannel {
    private long guildId;
    private long channelId;
    private String locale; // e.g., "de", "en"
  }
}

package org.bytewright.bgmo.adapter.notification.discord.model;

import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.bytewright.bgmo.domain.model.data.AdapterData;

@Data
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class DiscordAdapterData extends AdapterData {
  private long forumId;
  private long forumPostThreadId;
  private long guildId;
  private UUID meetupId;

  @Override
  public String getDataIdentifier() {
    return generateIdentifier(getGuildId(), getForumId(), getMeetupId());
  }

  public static String generateIdentifier(long guildId, long threadId, UUID meetupId) {
    return "%s-%d-%d".formatted(meetupId, threadId, guildId);
  }
}

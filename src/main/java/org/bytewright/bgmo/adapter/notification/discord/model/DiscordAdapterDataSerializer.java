package org.bytewright.bgmo.adapter.notification.discord.model;

import java.util.UUID;
import org.bytewright.bgmo.adapter.notification.discord.DiscordNotificationAdapter;
import org.bytewright.bgmo.domain.service.JsonMapperFactory;
import org.bytewright.bgmo.domain.service.data.AdapterDataSerializer;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class DiscordAdapterDataSerializer implements AdapterDataSerializer<DiscordAdapterData> {
  private static final JsonMapper mapper = JsonMapperFactory.unRedactedMapper();

  @Override
  public String getAdapterName() {
    return DiscordNotificationAdapter.DISCORD_ADAPTER.stableName();
  }

  @Override
  public DiscordAdapterData toConcreteType(AdapterDataRecord record) {
    Payload payload = readPayload(record.data());
    return DiscordAdapterData.builder()
        .id(record.id())
        .adapterName(record.adapterName())
        .tsCreation(record.tsCreation())
        .tsModified(record.tsModified())
        .guildId(payload.guildId())
        .forumId(payload.forumId())
        .forumPostThreadId(payload.forumPostThreadId())
        .meetupId(payload.meetupId())
        .build();
  }

  @Override
  public AdapterDataRecord fromConcreteType(DiscordAdapterData model) {
    Payload payload =
        new Payload(
            model.getGuildId(),
            model.getForumId(),
            model.getForumPostThreadId(),
            model.getMeetupId());
    return new AdapterDataRecord(
        model.getId(),
        model.getAdapterName(),
        model.getDataIdentifier(),
        writePayload(payload),
        model.getTsCreation(),
        model.getTsModified());
  }

  private Payload readPayload(String json) {
    try {
      return mapper.readValue(json, Payload.class);
    } catch (JacksonException e) {
      throw new IllegalStateException("Failed to deserialize DiscordAdapterData payload", e);
    }
  }

  private String writePayload(Payload payload) {
    try {
      return mapper.writeValueAsString(payload);
    } catch (JacksonException e) {
      throw new IllegalStateException("Failed to serialize DiscordAdapterData payload", e);
    }
  }

  // Only the 4 Discord-specific fields — no id/adapterName/timestamps duplication.
  private record Payload(long guildId, long forumId, long forumPostThreadId, UUID meetupId) {}
}

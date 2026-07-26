package org.bytewright.bgmo.adapter.notification.discord;

import static org.bytewright.bgmo.adapter.notification.discord.DiscordNotificationAdapter.DISCORD_ADAPTER;
import static org.bytewright.bgmo.domain.model.notification.NotificationContext.Content.*;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bytewright.bgmo.adapter.notification.discord.DiscordSettings.ForumChannel;
import org.bytewright.bgmo.adapter.notification.discord.model.DiscordAdapterData;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.domain.model.notification.NotificationContext;
import org.bytewright.bgmo.domain.service.data.AdapterDataDao;
import org.bytewright.bgmo.domain.service.data.ModelDao;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DiscordDataService {
  private final AdapterDataDao adapterDataDao;
  private final ModelDao<MeetupJoinRequest> joinRequestDao;

  Optional<UUID> findMeetupId(NotificationContext context) {
    UUID meetupId =
        switch (context.payload()) {
          case JoinRequestApproved joinRequestApproved ->
              joinRequestDao.findOrThrow(joinRequestApproved.joinRequestId()).getMeetupId();
          case JoinRequestCreated joinRequestCreated ->
              joinRequestDao.findOrThrow(joinRequestCreated.joinRequestId()).getMeetupId();
          case MeetupCanceled meetupCanceled -> meetupCanceled.meetupId();
          case MeetupCreated meetupCreated -> meetupCreated.meetupId();
          case MeetupRescheduled meetupRescheduled -> meetupRescheduled.meetupId();
          default -> throw new IllegalStateException("Unexpected value: " + context.payload());
        };
    return Optional.ofNullable(meetupId);
  }

  public Optional<DiscordAdapterData> findDataForMeetup(ForumChannel channel, UUID meetupId) {
    String identifier =
        DiscordAdapterData.generateIdentifier(
            channel.getGuildId(), channel.getChannelId(), meetupId);
    return adapterDataDao.findByAdapterAndIdentifier(DISCORD_ADAPTER, identifier);
  }

  public DiscordAdapterData createAndPersistNew(
      ForumChannel channel, long forumPostThreadId, UUID meetupId) {
    DiscordAdapterData discordAdapterData =
        DiscordAdapterData.builder()
            .adapterName(DISCORD_ADAPTER.stableName())
            .guildId(channel.getGuildId())
            .forumId(channel.getChannelId())
            .forumPostThreadId(forumPostThreadId)
            .meetupId(meetupId)
            .build();
    return adapterDataDao.persist(discordAdapterData);
  }
}

package org.bytewright.bgmo.adapter.notification.discord.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.bytewright.bgmo.adapter.notification.discord.DiscordNotificationAdapter;
import org.bytewright.bgmo.domain.service.data.AdapterDataDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AdapterDataEntityMapperIT {

  @Autowired private AdapterDataDao adapterDataDao;

  @Test
  void roundTripsDiscordAdapterDataThroughPersistenceAndBack() {
    DiscordAdapterData toPersist =
        DiscordAdapterData.builder()
            .adapterName(DiscordNotificationAdapter.DISCORD_ADAPTER.stableName())
            .guildId(111L)
            .forumId(222L).forumPostThreadId(333L)
            .meetupId(UUID.randomUUID())
            .build();

    DiscordAdapterData persisted = adapterDataDao.persist(toPersist);

    assertThat(persisted.getId()).isNotNull();
    assertThat(persisted.getTsCreation()).isNotNull();
    assertThat(persisted.getAdapterName())
        .isEqualTo(DiscordNotificationAdapter.DISCORD_ADAPTER.stableName());
    assertThat(persisted.getGuildId()).isEqualTo(111L);
    assertThat(persisted.getForumId()).isEqualTo(222L);
    assertThat(persisted.getForumPostThreadId()).isEqualTo(333L);
    assertThat(persisted.getMeetupId()).isEqualTo(toPersist.getMeetupId());

    Optional<DiscordAdapterData> found =
        adapterDataDao.findByAdapterAndIdentifier(
            DiscordNotificationAdapter.DISCORD_ADAPTER, persisted.getDataIdentifier());

    assertThat(found).isPresent();
    assertThat(found.get())
        .usingRecursiveComparison(
            RecursiveComparisonConfiguration.builder()
                .withIgnoredFields("tsCreation", "tsModified")
                .build())
        .isEqualTo(persisted);
  }

  @Test
  void findByAdapterAndIdentifierReturnsEmptyWhenNoMatch() {
    Optional<DiscordAdapterData> found =
        adapterDataDao.findByAdapterAndIdentifier(
            DiscordNotificationAdapter.DISCORD_ADAPTER, "does-not-exist");

    assertThat(found).isEmpty();
  }
}

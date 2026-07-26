package org.bytewright.bgmo.adapter.notification.discord;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bytewright.bgmo.domain.model.notification.NotificationContext;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatcherAnnouncements {
  private final MessageSource messageSource;
  private final ApiManager apiManager;

  public void dispatchToAnnouncements(DiscordSettings settings, NotificationContext context) {
    for (var announcementChannel : settings.getAnnouncementChannels()) {
      TextChannel channel =
          apiManager.getJda().getTextChannelById(announcementChannel.getChannelId());
      if (channel == null) {
        log.error(
            "Configured Discord group channel {} not found/visible to bot",
            announcementChannel.getChannelId());
        continue;
      }
      EmbedBuilder embed = buildEmbed(context, Locale.of(announcementChannel.getLocale()));
      channel
          .sendMessageEmbeds(embed.build())
          .queue(
              message -> onAnnouncementPosted(context, announcementChannel, message),
              failure -> log.error("Failed to send Discord group notification", failure));
    }
  }

  private void onAnnouncementPosted(
      NotificationContext context,
      DiscordSettings.AnnouncementChannel announcementChannel,
      Message message) {
    log.info(
        "Successfully posted message of type '{}' to channel: {}",
        context.payload().getClass().getSimpleName(),
        announcementChannel.getChannelId());
  }

  private EmbedBuilder buildEmbed(NotificationContext context, Locale locale) {
    NotificationContext.Content payload = context.payload();
    if (payload.isUsingI18N()) {
      return new EmbedBuilder()
          .setDescription(messageSource.getMessage(payload.messageKey(), null, locale));
    }
    return new EmbedBuilder().setDescription(context.payload().messageKey());
  }
}

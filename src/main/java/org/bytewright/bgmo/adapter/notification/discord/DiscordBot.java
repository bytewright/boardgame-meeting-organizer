package org.bytewright.bgmo.adapter.notification.discord;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.bytewright.bgmo.domain.model.notification.LinkingAttempt;
import org.bytewright.bgmo.domain.model.notification.NotificationChannel;
import org.bytewright.bgmo.domain.model.notification.NotificationContext;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.domain.service.notification.NotificationLinkCodeService;
import org.bytewright.bgmo.domain.service.notification.NotificationManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordBot extends ListenerAdapter {
  static final String CMD_ANNOUNCE_HERE = "announce_here";
  static final String CMD_STOP_ANNOUNCE_HERE = "stop_announcing_here";

  private final NotificationLinkCodeService linkCodeService;
  private final NotificationManager notificationManager;
  private final RegisteredUserDao userDao;

  private DiscordNotificationAdapter adapter;

  @Override
  public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
    // Only allow commands to be executed inside a server (guild)
    if (!event.isFromGuild()) {
      event.reply("This command can only be used in a server channel.").setEphemeral(true).queue();
      return;
    }
    String commandName = event.getName();
    long guildId = event.getGuild().getIdLong();
    long channelId = event.getChannel().getIdLong();

    if (CMD_ANNOUNCE_HERE.equals(commandName)) {
      OptionMapping localeOption = event.getOption("locale");
      String locale = localeOption != null ? localeOption.getAsString() : "de";

      boolean result = adapter.addAnnouncementChannel(guildId, channelId, locale);
      if (!result) {
        event.reply("Channel couldn't be found - check visibility permissions!").queue();
        return;
      }
      event
          .reply("Announcements will now be posted in this channel (Locale: " + locale + ").")
          .queue();
    } else if (CMD_STOP_ANNOUNCE_HERE.equals(commandName)) {
      adapter.removeAnnouncementChannel(guildId, channelId);
      event.reply("Announcements will no longer be posted in this channel.").queue();
    }
  }

  @Override
  public void onMessageReceived(MessageReceivedEvent event) {
    if (event.getChannelType() != ChannelType.PRIVATE) {
      return;
    }
    if (event.getAuthor().isBot()) {
      return;
    }
    handleIncomingDirectMessage(event);
  }

  private void handleIncomingDirectMessage(MessageReceivedEvent event) {
    String text = event.getMessage().getContentRaw();
    User otherUser = event.getAuthor();
    log.info("Bot received a DM from {}: {}", otherUser.getId(), text);

    if (!text.startsWith(APP_NAME_SHORT + "-")) {
      return;
    }

    var channel = new NotificationChannel.Discord(otherUser.getIdLong());
    var contact =
        ContactInfo.DiscordContact.builder()
            .userId(otherUser.getIdLong())
            .username(otherUser.getName())
            .build();

    var result =
        linkCodeService.attemptMessengerLinking(text, ContactInfoType.DISCORD, channel, contact);

    switch (result) {
      case LinkingAttempt.Failed ignored -> {
        log.error("Failed to link Discord account for code: {}", text);
        event
            .getChannel()
            .sendMessage(
                "That didn't work — the code may be too old. Grab a fresh one from the website and try again!")
            .queue();
      }
      case LinkingAttempt.LinkAndContactOption linkAndContactOption ->
          notifyLinked(linkAndContactOption.userId(), otherUser);
      case LinkingAttempt.Success success -> notifyLinked(success.userId(), otherUser);
    }
  }

  private void notifyLinked(UUID userId, User discordUser) {
    RegisteredUser user = userDao.findOrThrow(userId);
    NotificationContext response =
        NotificationContext.builder()
            .target(
                NotificationContext.Target.User.builder()
                    .userId(user.id())
                    .displayName(user.getDisplayName())
                    .channel(new NotificationChannel.Discord(discordUser.getIdLong()))
                    .build())
            .payload(
                NotificationContext.Content.UserMessengerLinked.builder()
                    .username(user.getDisplayName())
                    .build())
            .locale(Optional.ofNullable(user.getPreferredLocale()).orElse(Locale.ENGLISH))
            .build();
    notificationManager.dispatch(response);
  }

  @Override
  public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
    String componentId = event.getComponentId();
    if (componentId.startsWith("join:")) {
      UUID meetupId = UUID.fromString(componentId.split(":")[1]);
      long discordUserId = event.getUser().getIdLong();
      adapter.handleJoinRequestFromChat(meetupId, discordUserId);
      event.reply("Join request sent!").setEphemeral(true).queue();
    }
  }

  void setAdapter(DiscordNotificationAdapter adapter) {
    this.adapter = adapter;
  }
}

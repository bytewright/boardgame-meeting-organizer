package org.bytewright.bgmo.adapter.notification.discord;

import static org.bytewright.bgmo.adapter.notification.discord.DiscordBot.*;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiManager implements DisposableBean {
  private final DiscordAdapterProperties adapterProperties;
  private final DiscordBot discordBot;

  @Getter private JDA jda;

  public void registerBot() {
    if (jda != null) {
      log.info("Skipping bot registration as its already registered");
      return;
    }
    try {
      jda =
          JDABuilder.createDefault(
                  adapterProperties.getBotToken(),
                  GatewayIntent.DIRECT_MESSAGES,
                  GatewayIntent.GUILD_MESSAGES)
              .addEventListeners(discordBot)
              .build();
      jda.awaitReady();
      // Register the global slash commands
      jda.updateCommands()
          .addCommands(
              Commands.slash(CMD_ANNOUNCE_HERE, "Start posting announcements in this channel")
                  .addOption(
                      OptionType.STRING,
                      "locale",
                      "Language for the announcements (e.g., de, en)",
                      false),
              Commands.slash(CMD_STOP_ANNOUNCE_HERE, "Stop posting announcements in this channel"),
              Commands.slash(
                      CMD_USE_FORUM_CHANNEL, "Define the forum the post should create threads in")
                  .addOption(
                      OptionType.CHANNEL,
                      CMD_USE_FORUM_CHANNEL_ARG_CHANNEL,
                      "id of forum channel",
                      true)
                  .addOption(
                      OptionType.STRING,
                      CMD_USE_FORUM_CHANNEL_ARG_LOCALE,
                      "Language for the announcements (e.g., de, en). Defaults to DE",
                      false))
          .queue(
              success -> log.info("Successfully registered Discord slash commands."),
              failure -> log.error("Failed to register Discord slash commands.", failure));
      log.info("Discord bot is ready.");
    } catch (Exception e) {
      log.error("Discord bot failed to initialize with error: {}", e.getMessage(), e);
    }
  }

  public void unregisterBot() {
    if (jda != null) {
      jda.shutdown();
      try {
        jda.awaitShutdown();
      } catch (InterruptedException e) {
        log.error("Error while waiting for JDA to shutdown", e);
      }
      jda = null;
    }
  }

  @Override
  public void destroy() throws Exception {
    unregisterBot();
  }
}

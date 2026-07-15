package org.bytewright.bgmo.adapter.notification.discord;

import static org.bytewright.bgmo.adapter.notification.discord.DiscordBot.CMD_ANNOUNCE_HERE;
import static org.bytewright.bgmo.adapter.notification.discord.DiscordBot.CMD_STOP_ANNOUNCE_HERE;
import static org.bytewright.bgmo.domain.model.notification.NotificationContext.*;
import static org.bytewright.bgmo.domain.model.notification.NotificationContext.Content.*;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bytewright.bgmo.domain.model.AdapterSettings;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.notification.NotificationChannel;
import org.bytewright.bgmo.domain.model.notification.NotificationContext;
import org.bytewright.bgmo.domain.model.notification.VerificationStep;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.AdapterSettingsProvider;
import org.bytewright.bgmo.domain.service.data.AdapterSettingsDao;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.domain.service.notification.ChatBotNotificationTaskExecutor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordNotificationAdapter
    implements ChatBotNotificationTaskExecutor,
        AdapterSettingsProvider,
        InitializingBean,
        DisposableBean,
        ApplicationListener<ApplicationReadyEvent> {

  private static final String ADAPTER_NAME = "Discord-ChatBotNotificationTaskExecutor-integration";
  private final DiscordAdapterProperties adapterProperties;
  private final AdapterSettingsDao adapterSettingsDao;
  private final RegisteredUserDao userDao;
  private final MessageSource messageSource;
  private final DiscordBot discordBot;
  private final MeetupDao meetupDao;
  private final JsonMapper objectMapper;

  private JDA jda;
  private boolean isRegistered = false;

  @Override
  public boolean supports(NotificationContext context) {
    return switch (context.target()) {
      case Target.Group ignored -> true;
      case Target.Anon anon -> isChannelDiscord(anon.channel());
      case Target.User user -> isChannelDiscord(user.channel());
    };
  }

  private boolean isChannelDiscord(NotificationChannel channel) {
    return channel instanceof NotificationChannel.Discord;
  }

  @Override
  public boolean isContactHandlerFor(ContactInfoType type) {
    return type == ContactInfoType.DISCORD;
  }

  @Override
  @Async
  public void execute(NotificationContext context) {
    String messageKey = context.payload().messageKey();
    if (!isEnabled()) {
      log.info("Discord integration is disabled, skipping execution of: {}", messageKey);
      return;
    }

    switch (context.target()) {
      case Target.Group ignored -> sendToGroupChannels(context);
      case Target.User user -> sendToUser(user, context);
      default ->
          throw new UnsupportedOperationException(
              "Can't send messages to target: " + context.target());
    }
  }

  private void sendToGroupChannels(NotificationContext context) {
    DiscordSettings settings = getSettings();
    for (var announcementChannel : settings.getAnnouncementChannels()) {
      TextChannel channel = jda.getTextChannelById(announcementChannel.getChannelId());
      if (channel == null) {
        log.error(
            "Configured Discord group channel {} not found/visible to bot",
            announcementChannel.getChannelId());
        return;
      }
      EmbedBuilder embed = buildEmbed(context, Locale.of(announcementChannel.getLocale()));
      channel
          .sendMessageEmbeds(embed.build())
          .queue(
              message ->
                  log.info(
                      "Successfully posted message to channel: {}",
                      announcementChannel.getChannelId()),
              failure -> log.error("Failed to send Discord group notification", failure));
    }
  }

  private void sendToUser(Target.User userTarget, NotificationContext context) {
    long discordUserId = extractDiscordUserId(context);
    EmbedBuilder embed = buildEmbed(context);
    jda.retrieveUserById(discordUserId)
        .queue(
            user ->
                user.openPrivateChannel()
                    .queue(
                        dm ->
                            dm.sendMessageEmbeds(embed.build())
                                .queue(
                                    null,
                                    failure ->
                                        log.error(
                                            "Failed to DM Discord user {}, app user id: {}",
                                            discordUserId,
                                            userTarget,
                                            failure)),
                        failure ->
                            log.error(
                                "Failed to open DM channel with Discord user {}",
                                discordUserId,
                                failure)),
            failure -> log.error("Failed to resolve Discord user {}", discordUserId, failure));
  }

  private long extractDiscordUserId(NotificationContext context) {
    return context
        .extractChannel()
        .filter(NotificationChannel.Discord.class::isInstance)
        .map(NotificationChannel.Discord.class::cast)
        .map(NotificationChannel.Discord::userId)
        .orElseThrow();
  }

  private EmbedBuilder buildEmbed(NotificationContext context) {
    return buildEmbed(context, context.locale() != null ? context.locale() : Locale.GERMAN);
  }

  private EmbedBuilder buildEmbed(NotificationContext context, Locale locale) {
    return switch (context.payload()) {
      case MeetupCreated meetupCreated -> createMeetupCreated(meetupCreated, locale);
      default -> {
        Content payload = context.payload();
        if (payload.isUsingI18N()) {
          yield new EmbedBuilder()
              .setDescription(messageSource.getMessage(payload.messageKey(), null, locale));
        }
        yield new EmbedBuilder().setDescription(context.payload().messageKey());
      }
    };
  }

  private EmbedBuilder createMeetupCreated(MeetupCreated notificationContext, Locale locale) {
    MeetupEvent meetupEvent = meetupDao.findOrThrow(notificationContext.meetupId());
    String formattedDate =
        meetupEvent
            .getEventDate()
            .format(DateTimeFormatter.ofPattern("EEE dd.MM.yyyy, HH:mm", locale));
    UUID creatorId = meetupEvent.getCreatorId();
    RegisteredUser creator = userDao.findOrThrow(creatorId);
    URL url = notificationContext.meetupUrl();
    String location =
        messageSource.getMessage("bgmo.adapter.notification.discord.location-title", null, locale);
    String date =
        messageSource.getMessage("bgmo.adapter.notification.discord.date-title", null, locale);
    return new EmbedBuilder()
        .setTitle(notificationContext.title(), url.toString())
        .addField(location, meetupEvent.getAreaHint(), true)
        .addField(date, formattedDate, true)
        .setDescription(meetupEvent.getDescription())
        .setAuthor(creator.getDisplayName());
  }

  @Override
  public void afterPropertiesSet() {
    discordBot.setAdapter(this);
  }

  private void registerBot() {
    try {
      if (!isEnabled()) {
        log.warn("Discord bot is disabled, skipping registration.");
        return;
      }
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
              Commands.slash(CMD_STOP_ANNOUNCE_HERE, "Stop posting announcements in this channel"))
          .queue(
              success -> log.info("Successfully registered Discord slash commands."),
              failure -> log.error("Failed to register Discord slash commands.", failure));
      isRegistered = true;
      log.info("Discord bot is ready.");
    } catch (Exception e) {
      log.error("Discord bot failed to initialize with error: {}", e.getMessage(), e);
    }
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    registerBot();
  }

  void handleJoinRequestFromChat(UUID meetupId, long discordUserId) {
    log.info(
        "Received a join request from Discord for meetup {} from user {}", meetupId, discordUserId);
  }

  @Override
  public boolean isEnabled() {
    DiscordSettings settings = getSettings();
    return adapterProperties.isEnabled() && settings.isEnabled();
  }

  @Override
  public String botChatDisplayName() {
    return adapterProperties.getBotDisplayName();
  }

  @Override
  public Optional<String> generateBotDeepLink() {
    // TODO(discord): this is NOT equivalent to Telegram's deep link — Telegram's opens a
    // DM directly; this opens Discord's "add bot to server" OAuth flow. The linking steps
    // below need to walk the user through joining that server *first*, then DMing the
    // code — reflect this difference clearly in the UI copy, don't just relabel the
    // Telegram button.
    return Optional.of(
        "https://discord.com/api/oauth2/authorize?client_id="
            + adapterProperties.getClientId()
            + "&scope=bot%20applications.commands&permissions=2048");
    // permissions=2048 is Send Messages. TODO(discord): add Embed Links (16384) if not
    // already covered by a broader default — check the actual computed permission bits
    // via Discord's own permissions calculator in the dev portal rather than trusting this
    // number blindly.
  }

  @Override
  public List<VerificationStep> generateLinkingSteps() {
    // TODO currently mock, needs real steps to link discord
    DiscordSettings settings = getSettings();
    return List.of(
        VerificationStep.builder().messageKey("adapter.discord.tutorial.step1").build(),
        VerificationStep.builder()
            .messageKey("adapter.discord.tutorial.step2")
            .pictureUrl(settings.getTutorialStep2Link())
            .build(),
        VerificationStep.builder()
            .messageKey("adapter.discord.tutorial.step3")
            .pictureUrl(settings.getTutorialStep3Link())
            .build());
  }

  private DiscordSettings getSettings() {
    AdapterSettings adapterSettings = adapterSettingsDao.findByAdapter(getAdapterInfo());
    return getSettings(adapterSettings);
  }

  private DiscordSettings getSettings(AdapterSettings settings) {
    try {
      return objectMapper.readValue(settings.getAdapterSettings(), DiscordSettings.class);
    } catch (Exception e) {
      log.error(
          "Error while fetching Discord adapter settings, falling back to default! {}",
          e.getMessage());
      return DiscordSettings.builder().build();
    }
  }

  private void saveSettings(DiscordSettings settings) {
    AdapterSettings adapterSettings = adapterSettingsDao.findByAdapter(getAdapterInfo());
    try {
      String json = objectMapper.writeValueAsString(settings);
      adapterSettings.setAdapterSettings(json);
      if (isValidSettingsJson(json) == ValidationResult.VALID) {
        adapterSettingsDao.createOrUpdate(adapterSettings);
      } else
        throw new IllegalArgumentException(
            "Failed to save adapter settings, json validation failed");
    } catch (Exception e) {
      log.error("Failed to save DiscordSettings", e);
    }
  }

  @Override
  public void onUpdate(AdapterSettings updatedSettings) {
    DiscordSettings settings = getSettings(updatedSettings);
    if (settings.isEnabled() && !isRegistered) {
      registerBot();
    }

    if (!settings.isEnabled() && isRegistered) {
      isRegistered = false;
    }
  }

  @Override
  public String getDefaultSettings() throws JacksonException {
    return objectMapper.writeValueAsString(DiscordSettings.builder().build());
  }

  @Override
  public AdapterInfo getAdapterInfo() {
    return AdapterInfo.builder()
        .stableName(ADAPTER_NAME)
        .description("Discord integration, sends notifications and updates to linked users")
        .build();
  }

  @Override
  public ValidationResult isValidSettingsJson(String jsonData) {
    try {
      DiscordSettings settings = objectMapper.readValue(jsonData, DiscordSettings.class);
      return settings != null ? ValidationResult.VALID : ValidationResult.INVALID;
    } catch (JacksonException e) {
      log.error("Error while validating json: {}", e.getMessage());
    }
    return ValidationResult.INVALID;
  }

  @Override
  public void destroy() {
    if (jda != null) {
      log.info("Shutting down discord adapter");
      jda.shutdown();
    }
  }

  public boolean addAnnouncementChannel(long guildId, long channelId, String locale) {
    TextChannel channel = jda.getTextChannelById(channelId);
    if (channel == null) {
      log.warn(
          "Received new announcementChannel registration but channel can not be found by jda!");
      return false;
    }
    DiscordSettings settings = getSettings();

    // Ensure mutable list
    List<DiscordSettings.AnnouncementChannel> channels =
        new ArrayList<>(settings.getAnnouncementChannels());

    // Remove if already exists to update locale
    channels.removeIf(c -> c.getGuildId() == guildId && c.getChannelId() == channelId);
    var announcementChannel =
        DiscordSettings.AnnouncementChannel.builder()
            .guildId(guildId)
            .channelId(channelId)
            .locale(locale)
            .build();
    log.info("Adding new discord announcing channel: {}", announcementChannel);
    channels.add(announcementChannel);

    settings.setAnnouncementChannels(channels);
    saveSettings(settings);
    return true;
  }

  public void removeAnnouncementChannel(long guildId, long channelId) {
    DiscordSettings settings = getSettings();
    List<DiscordSettings.AnnouncementChannel> channels =
        new ArrayList<>(settings.getAnnouncementChannels());

    channels.removeIf(c -> c.getGuildId() == guildId && c.getChannelId() == channelId);

    log.info("Removing discord announcing channel: guidId={}, channelId={}", guildId, channelId);
    settings.setAnnouncementChannels(channels);
    saveSettings(settings);
  }
}

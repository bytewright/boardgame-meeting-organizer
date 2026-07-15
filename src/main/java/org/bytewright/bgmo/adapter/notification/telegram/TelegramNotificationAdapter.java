package org.bytewright.bgmo.adapter.notification.telegram;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramNotificationAdapter
    implements ChatBotNotificationTaskExecutor,
        AdapterSettingsProvider,
        InitializingBean,
        DisposableBean,
        ApplicationListener<ApplicationReadyEvent> {
  private static final String ADAPTER_NAME = "Telegram-ChatBotNotificationTaskExecutor-integration";
  private final TelegramAdapterProperties adapterProperties;
  private final TelegramTemplateService templateService;
  private final TelegramContactRenderer contactRenderer;
  private final AdapterSettingsDao adapterSettingsDao;
  private final TelegramBot telegramBot;
  private final RegisteredUserDao userDao;
  private final MeetupDao meetupDao;
  private final JsonMapper objectMapper;
  private TelegramBotsLongPollingApplication botsApp;
  private boolean isRegistered = false;

  @Override
  public boolean supports(NotificationContext context) {
    return switch (context.target()) {
      case NotificationContext.Target.Group ignored ->
          StringUtils.hasText(adapterProperties.getGroupChatId());
      case NotificationContext.Target.Anon anon -> isChannelTelegram(anon.channel());
      case NotificationContext.Target.User user -> isChannelTelegram(user.channel());
    };
  }

  private boolean isChannelTelegram(NotificationChannel channel) {
    return channel instanceof NotificationChannel.Telegram;
  }

  @Override
  public boolean isContactHandlerFor(ContactInfoType type) {
    return type == ContactInfoType.TELEGRAM;
  }

  @Override
  @Async
  public void execute(NotificationContext context) {
    String messageKey = context.payload().messageKey();
    if (!isEnabled()) {
      log.info("Telegram integration is disabled, skipping execution of: {}", messageKey);
      return;
    }
    execute(context, getChatId(context));
  }

  void execute(NotificationContext context, String chatId) {
    String renderedMessage = renderMessage(context);
    SendMessage message =
        SendMessage.builder().chatId(chatId).text(renderedMessage).parseMode("MarkdownV2").build();

    // Adding the "Join" button if a meetupId is present
    if (false
        && context.payload() instanceof NotificationContext.Content.MeetupCreated meetupCreated) {
      var button =
          InlineKeyboardButton.builder()
              .text("Join Meetup 🎲")
              .callbackData("join:" + meetupCreated.meetupId())
              .build();
      message.setReplyMarkup(
          InlineKeyboardMarkup.builder()
              .keyboardRow(new InlineKeyboardRow(List.of(button)))
              .build());
    }

    try {
      telegramBot.execute(message);
    } catch (TelegramApiException e) {
      log.error("Failed to send Telegram notification", e);
    }
  }

  private String renderMessage(NotificationContext context) {
    Locale targetLocale = context.locale() != null ? context.locale() : Locale.GERMAN;
    if (context.payload()
        instanceof
        NotificationContext.Content.MeetupCreated(
            String title,
            UUID meetupId,
            java.net.URL meetupUrl)) {
      MeetupEvent meetupEvent = meetupDao.findOrThrow(meetupId);
      RegisteredUser creator = userDao.findOrThrow(meetupEvent.getCreatorId());
      Duration uniSecondsSinceEpoch =
          Duration.between(Instant.EPOCH, meetupEvent.getEventDate().toInstant());

      String formattedDate =
          meetupEvent
              .getEventDate()
              .format(DateTimeFormatter.ofPattern("EEE., dd.MM.yyyy 'at' HH:mm", targetLocale));

      Map<String, Object> vars =
          Map.of(
              "title",
              title,
              "meetupUrl",
              meetupUrl,
              "organizerDeeplink",
              contactRenderer.render(creator),
              "eventDate",
              formattedDate,
              "location",
              meetupEvent.getAreaHint(),
              "description",
              Objects.requireNonNullElse(meetupEvent.getDescription(), ""));
      return templateService.render(targetLocale, context.payload().messageKey(), vars);
    }
    return templateService.render(targetLocale, context.payload());
  }

  private String getChatId(NotificationContext context) {
    return switch (context.target()) {
      case NotificationContext.Target.Group ignored -> adapterProperties.getGroupChatId();
      default -> Long.toString(extractChatId(context));
    };
  }

  private long extractChatId(NotificationContext context) {
    return context
        .extractChannel()
        .filter(nc -> NotificationChannel.Telegram.class.isAssignableFrom(nc.getClass()))
        .map(NotificationChannel.Telegram.class::cast)
        .map(NotificationChannel.Telegram::chatId)
        .orElseThrow();
  }

  @Override
  public void afterPropertiesSet() {
    telegramBot.setAdapter(this);
    botsApp = new TelegramBotsLongPollingApplication();
  }

  void handleJoinRequestFromChat(UUID meetupId, String telegramChatId) {
    log.info(
        "Received a join request from telegram for meetup {} from chatId {}",
        meetupId,
        telegramChatId);
    // todo create request
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    registerBot();
  }

  private void registerBot() {
    try {
      if (!isEnabled()) {
        log.warn("Telegram bot is disabled, skipping registering with external service.");
        return;
      }
      botsApp.registerBot(adapterProperties.getBotToken(), telegramBot);
      isRegistered = true;
      log.info("Telegram bot is ready, listening to chat updates...");
    } catch (Exception e) {
      log.error("Telegram bot failed to initialize with error: {}", e.getMessage(), e);
    }
  }

  @Override
  public boolean isEnabled() {
    TelegramSettings settings = getSettings();
    return adapterProperties.isEnabled() && settings.isEnabled();
  }

  @Override
  public String botChatDisplayName() {
    return adapterProperties.getBotDisplayName();
  }

  @Override
  public Optional<String> generateBotDeepLink() {
    return Optional.of("https://t.me/%s".formatted(adapterProperties.getBotUsername()));
  }

  @Override
  public List<VerificationStep> generateLinkingSteps() {
    TelegramSettings settings = getSettings();
    return List.of(
        VerificationStep.builder().messageKey("adapter.telegram.tutorial.step1").build(),
        VerificationStep.builder()
            .messageKey("adapter.telegram.tutorial.step2")
            .pictureUrl(settings.getTutorialStep2Link())
            .build(),
        VerificationStep.builder()
            .messageKey("adapter.telegram.tutorial.step3")
            .pictureUrl(settings.getTutorialStep3Link())
            .build());
  }

  private TelegramSettings getSettings() {
    AdapterSettings adapterSettings = adapterSettingsDao.findByAdapter(getAdapterInfo());
    return getSettings(adapterSettings);
  }

  private TelegramSettings getSettings(AdapterSettings settings) {
    try {
      return objectMapper.readValue(settings.getAdapterSettings(), TelegramSettings.class);
    } catch (Exception e) {
      log.error(
          "Error while fetching adapter settings, falling back to default! {}", e.getMessage());
      return TelegramSettings.builder().build();
    }
  }

  @Override
  public void onUpdate(AdapterSettings updatedSettings) {
    TelegramSettings settings = getSettings(updatedSettings);
    if (settings.isEnabled() && !isRegistered) {
      registerBot();
    }
    if (!settings.isEnabled() && isRegistered) {
      try {
        botsApp.unregisterBot(adapterProperties.getBotToken());
      } catch (TelegramApiException e) {
        log.error("Failed to unregister bot!", e);
      }
    }
  }

  @Override
  public String getDefaultSettings() throws JacksonException {
    return objectMapper.writeValueAsString(TelegramSettings.builder().build());
  }

  @Override
  public AdapterInfo getAdapterInfo() {
    return AdapterInfo.builder()
        .stableName(ADAPTER_NAME)
        .description(
            "Telegram messenger API integration, sends notifications and updates to linked users")
        .build();
  }

  @Override
  public ValidationResult isValidSettingsJson(String jsonData) {
    try {
      TelegramSettings telegramSettings = objectMapper.readValue(jsonData, TelegramSettings.class);
      return telegramSettings != null ? ValidationResult.VALID : ValidationResult.INVALID;
    } catch (JacksonException e) {
      log.error("Error while validating json: {}", e.getMessage());
    }
    return ValidationResult.INVALID;
  }

  @Override
  public void destroy() throws Exception {
    botsApp.close();
  }

  String getBotToken() {
    return adapterProperties.getBotToken();
  }
}

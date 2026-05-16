package org.bytewright.bgmo.adapter.bot.telegram;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.AdapterSettings;
import org.bytewright.bgmo.domain.model.notification.NotificationContext;
import org.bytewright.bgmo.domain.model.notification.NotificationTargetType;
import org.bytewright.bgmo.domain.model.notification.VerificationStep;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.model.user.ContactOption;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.AdapterSettingsProvider;
import org.bytewright.bgmo.domain.service.data.AdapterSettingsDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.domain.service.notification.ChatBotNotificationTaskExecutor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
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
  private final AdapterSettingsDao adapterSettingsDao;
  private final MessageSource messageSource;
  private final TelegramBot telegramBot;
  private final RegisteredUserDao userDao;
  private final JsonMapper objectMapper;
  private TelegramBotsLongPollingApplication botsApp;

  @Override
  public boolean supports(NotificationContext context) {
    return context.notificationTargetType() == NotificationTargetType.GROUP
        || userDao.hasContactOfType(context.userId(), ContactInfoType.TELEGRAM);
  }

  @Override
  public boolean isContactHandlerFor(ContactInfoType type) {
    return type == ContactInfoType.TELEGRAM;
  }

  @Override
  @Async
  public void execute(NotificationContext context) {
    if (!isEnabled()) {
      log.info("Telegram integration is disabled, skipping execution of: {}", context.messageKey());
      return;
    }
    Locale targetLocale = context.locale() != null ? context.locale() : Locale.GERMAN;
    String rawMessage =
        messageSource.getMessage(
            context.messageKey(), context.messageArgs().toArray(new Object[0]), targetLocale);
    String formattedMessage = escapeMarkdown(rawMessage);
    SendMessage message =
        SendMessage.builder()
            .chatId(getChatId(context))
            .text(formattedMessage)
            .parseMode("MarkdownV2")
            .build();

    // Adding the "Join" button if a meetupId is present
    if (context.meetupId() != null) {
      var button =
          InlineKeyboardButton.builder()
              .text("Join Meetup 🎲")
              .callbackData("join:" + context.meetupId())
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

  private String getChatId(NotificationContext context) {
    return switch (context.notificationTargetType()) {
      case GROUP -> adapterProperties.getGroupChatId();
      case DIRECT -> {
        RegisteredUser user = userDao.findOrThrow(context.userId());
        List<ContactInfo.TelegramContact> infos =
            user.getContactOptions().stream()
                .map(ContactOption::getContactInfo)
                .filter(ContactInfo.TelegramContact.class::isInstance)
                .map(ContactInfo.TelegramContact.class::cast)
                .toList();
        if (infos.size() != 1) {
          throw new IllegalArgumentException(
              "Can't find unique ContactInfo for telegram for user " + user.logEntity());
        }
        yield infos.getFirst().chatId();
      }
    };
  }

  private String escapeMarkdown(String text) {
    // Telegram requires escaping chars like . - ! in MarkdownV2
    return text.replaceAll("([_\\*\\[\\]\\(\\)~`>#\\+\\-=|\\.!\\{\\}])", "\\\\$1");
  }

  @Override
  public void afterPropertiesSet() {
    telegramBot.setMeetUpJoinRequestHandler(this::handleJoinRequestFromChat);
    botsApp = new TelegramBotsLongPollingApplication();
  }

  private void handleJoinRequestFromChat(UUID meetupId, String telegramChatId) {
    log.info(
        "Received a join request from telegram for meetup {} from chatId {}",
        meetupId,
        telegramChatId);
    // todo create request
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    try {
      if (!isEnabled()) {
        log.warn("Telegram bot is disabled, skipping registering with external service.");
        return;
      }
      botsApp.registerBot(adapterProperties.getBotToken(), telegramBot);

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
    return Optional.of("https://t.me/bgmo_jena_bot");
  }

  @Override
  public List<VerificationStep> generateVerificationSteps() {
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
    try {
      AdapterSettings adapterSettings = adapterSettingsDao.findByAdapter(getAdapterInfo());
      return objectMapper.readValue(adapterSettings.getAdapterSettings(), TelegramSettings.class);
    } catch (Exception e) {
      log.error(
          "Error while fetching adapter settings, falling back to default! {}", e.getMessage());
      return TelegramSettings.builder().build();
    }
  }

  @Override
  public String getDefaultSettings() throws JsonProcessingException {
    return objectMapper.writeValueAsString(TelegramSettings.builder().build());
  }

  @Override
  public AdapterSettingsProvider.AdapterInfo getAdapterInfo() {
    return AdapterSettingsProvider.AdapterInfo.builder()
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
    } catch (tools.jackson.core.JacksonException e) {
      log.error("Error while validating json: {}", e.getMessage());
    }
    return ValidationResult.INVALID;
  }

  @Override
  public void destroy() throws Exception {
    botsApp.close();
  }
}

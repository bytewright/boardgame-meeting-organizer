package org.bytewright.bgmo.adapter.bot.telegram;

import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.notification.NotificationContext;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.service.notification.NotificationTaskExecutor;
import org.bytewright.bgmo.domain.service.notification.VerificationCodeService;
import org.bytewright.bgmo.usecases.MeetupWorkflows;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Service
public class TelegramNotificationAdapter extends TelegramLongPollingBot
    implements NotificationTaskExecutor {
  private final TelegramAdapterProperties adapterProperties;
  private final VerificationCodeService verificationService;
  private final MeetupWorkflows meetupWorkflows;
  private final MessageSource messageSource;

  public TelegramNotificationAdapter(
      TelegramAdapterProperties adapterProperties,
      VerificationCodeService verificationService,
      MeetupWorkflows meetupWorkflows,
      MessageSource messageSource) {
    super(adapterProperties.getBotToken());
    this.adapterProperties = adapterProperties;
    this.verificationService = verificationService;
    this.meetupWorkflows = meetupWorkflows;
    this.messageSource = messageSource;
  }

  @Override
  public String getBotUsername() {
    return adapterProperties.getBotUsername();
  }

  @Override
  public boolean supports(ContactInfoType type) {
    return type == ContactInfoType.TELEGRAM;
  }

  @Override
  @Async
  public void execute(NotificationContext context) {
    Locale targetLocale = context.locale() != null ? context.locale() : Locale.GERMAN;
    String rawMessage =
        messageSource.getMessage(context.messageKey(), context.messageArgs(), targetLocale);
    String formattedMessage = escapeMarkdown(rawMessage);
    SendMessage message = new SendMessage();
    message.setChatId(context.targetId());
    message.setText(formattedMessage);
    message.setParseMode("MarkdownV2");

    // Example: Adding the "Join" button if a meetupId is present
    if (context.metadata().containsKey("meetupId")) {
      var button =
          InlineKeyboardButton.builder()
              .text("Join Meetup 🎲")
              .callbackData("join:" + context.metadata().get("meetupId"))
              .build();
      message.setReplyMarkup(InlineKeyboardMarkup.builder().keyboardRow(List.of(button)).build());
    }

    try {
      execute(message);
    } catch (TelegramApiException e) {
      log.error("Failed to send Telegram notification", e);
    }
  }

  private String escapeMarkdown(String text) {
    // Telegram requires escaping chars like . - ! in MarkdownV2
    return text.replaceAll("([_\\*\\[\\]\\(\\)~`>#\\+\\-=|\\.!\\{\\}])", "\\\\$1");
  }

  @Override
  public void onUpdateReceived(Update update) {
    if (update.hasMessage() && update.getMessage().hasText()) {
      handleIncomingText(update.getMessage());
    } else if (update.hasCallbackQuery()) {
      handleCallback(update.getCallbackQuery());
    }
  }

  private void handleIncomingText(Message message) {
    String text = message.getText();
    if (text.startsWith("BGMO-")) {
      boolean success =
          verificationService.attemptVerification(
              text, ContactInfoType.TELEGRAM, message.getChatId().toString());
      if (success) {
        // Reply: "Account linked successfully!"
      }
    }
  }

  private void handleCallback(CallbackQuery query) {
    if (query.getData().startsWith("join:")) {
      String meetupId = query.getData().split(":")[1];
      // Identify user by query.getFrom().getId() and call meetupWorkflows.join(...)
      AnswerCallbackQuery answer = new AnswerCallbackQuery();
      answer.setCallbackQueryId(query.getId());
      answer.setText("Join request sent!");
      try {
        execute(answer);
      } catch (Exception ignored) {
      }
    }
  }
}

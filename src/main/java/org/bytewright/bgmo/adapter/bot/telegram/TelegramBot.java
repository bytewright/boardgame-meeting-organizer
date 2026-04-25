package org.bytewright.bgmo.adapter.bot.telegram;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import java.util.UUID;
import java.util.function.BiConsumer;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.service.notification.VerificationCodeService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Service
public class TelegramBot extends TelegramLongPollingBot {
  private final TelegramAdapterProperties adapterProperties;
  private final VerificationCodeService verificationService;
  @Setter private BiConsumer<UUID, String> meetUpJoinRequestHandler;

  public TelegramBot(
      TelegramAdapterProperties adapterProperties, VerificationCodeService verificationService) {
    super(adapterProperties.getBotToken());
    this.adapterProperties = adapterProperties;
    this.verificationService = verificationService;
    log.info("created bot: {}", adapterProperties);
  }

  @Override
  public String getBotUsername() {
    return adapterProperties.getBotUsername();
  }

  @Override
  public void onUpdateReceived(Update update) {
    log.info("Bot received an update: {}", update);
    if (update.hasMessage() && update.getMessage().hasText()) {
      handleIncomingText(update.getMessage());
    } else if (update.hasCallbackQuery()) {
      handleCallback(update.getCallbackQuery());
    }
  }

  private void handleIncomingText(Message message) {
    String text = message.getText();
    log.info("Received message from {}: {}", message.getFrom(), message);
    if (text.startsWith(APP_NAME_SHORT + "-")) {
      boolean success =
          verificationService.attemptVerification(
              text, ContactInfoType.TELEGRAM, message.getChatId().toString());
      if (success) {
        try {
          execute(
              SendMessage.builder()
                  .chatId(message.getChatId())
                  .text("Account linked successfully!")
                  .build());
        } catch (TelegramApiException e) {
          log.error("Failed to send user verification confirmation!");
        }
      }
    }
  }

  private void handleCallback(CallbackQuery query) {
    if (query.getData().startsWith("join:")) {
      UUID meetupId = UUID.fromString(query.getData().split(":")[1]);
      String id = query.getFrom().getId().toString();
      meetUpJoinRequestHandler.accept(meetupId, id);
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

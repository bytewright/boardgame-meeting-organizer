package org.bytewright.bgmo.adapter.bot.telegram;

import java.util.UUID;
import java.util.function.BiConsumer;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.service.notification.VerificationCodeService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Service
public class TelegramBot extends TelegramLongPollingBot {
  private final TelegramAdapterProperties adapterProperties;
  private final VerificationCodeService verificationService;
  @Setter private BiConsumer<UUID, String> meetUpJoinRequestHandler;

  public TelegramBot(
      TelegramAdapterProperties adapterProperties, VerificationCodeService verificationService) {
    // super(adapterProperties.getBotToken());
    this.adapterProperties = adapterProperties;
    this.verificationService = verificationService;
  }

  @Override
  public String getBotUsername() {
    return adapterProperties.getBotUsername();
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

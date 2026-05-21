package org.bytewright.bgmo.adapter.notification.telegram;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.notification.*;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.service.notification.VerificationCodeService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Service
public class TelegramBot implements LongPollingSingleThreadUpdateConsumer {
  @Setter(AccessLevel.PACKAGE)
  private TelegramNotificationAdapter adapter;

  private final TelegramClient telegramClient;
  private final VerificationCodeService verificationService;

  public TelegramBot(
      TelegramAdapterProperties adapterProperties, VerificationCodeService verificationService) {
    telegramClient = new OkHttpTelegramClient(adapterProperties.getBotToken());
    this.verificationService = verificationService;
  }

  @Override
  public void consume(Update update) {
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
      var result =
          verificationService.attemptMessengerVerification(
              text,
              ContactInfoType.TELEGRAM,
              message.getChatId().toString(),
              message.getChat().getUserName());
      try {
        switch (result) {
          case VerificationAttempt.Failed ignore -> {
            log.error("Failed to send user verification confirmation!");
            telegramClient.execute(
                SendMessage.builder()
                    .chatId(message.getChatId())
                    .text(
                        "Account linked did not work, maybe the code you sent is too old? Maybe try it with a fresh one from the website!")
                    .build());
          }
          case VerificationAttempt.Success success -> {
            NotificationContext response =
                NotificationContext.builder()
                    .notificationTargetType(NotificationTargetType.DIRECT)
                    .payload(
                        NotificationPayload.UserMessengerLinked.builder()
                            .username(success.user().getDisplayName())
                            .build())
                    .userId(success.user().getId())
                    .locale(success.user().getPreferredLocale())
                    .build();
            adapter.execute(response, Long.toString(message.getChatId()));
          }
        }
      } catch (TelegramApiException e) {
        log.error("Error when sending user verification result message!", e);
      }
    }
  }

  private void handleCallback(CallbackQuery query) {
    if (query.getData().startsWith("join:")) {
      UUID meetupId = UUID.fromString(query.getData().split(":")[1]);
      String id = query.getFrom().getId().toString();
      adapter.handleJoinRequestFromChat(meetupId, id);
      AnswerCallbackQuery answer = new AnswerCallbackQuery(query.getId());
      answer.setText("Join request sent!");
      try {
        telegramClient.execute(answer);
      } catch (Exception ignored) {
      }
    }
  }

  public void execute(SendMessage message) throws TelegramApiException {
    telegramClient.execute(message);
  }
}

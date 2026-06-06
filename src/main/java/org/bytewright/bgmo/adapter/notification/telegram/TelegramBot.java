package org.bytewright.bgmo.adapter.notification.telegram;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.notification.LinkingAttempt;
import org.bytewright.bgmo.domain.model.notification.NotificationChannel;
import org.bytewright.bgmo.domain.model.notification.NotificationContext;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.domain.service.notification.NotificationLinkCodeService;
import org.bytewright.bgmo.domain.service.notification.NotificationManager;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBot implements LongPollingSingleThreadUpdateConsumer {

  private final NotificationLinkCodeService linkCodeService;
  private final NotificationManager notificationManager;
  private final RegisteredUserDao userDao;

  private TelegramNotificationAdapter adapter;
  private TelegramClient telegramClient;

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
    User otherUser = message.getFrom();
    log.info("Received message from {}: {}", otherUser, message);
    if (text.startsWith(APP_NAME_SHORT + "-")) {
      var channel = new NotificationChannel.Telegram(message.getChatId(), otherUser.getId());
      var contact =
          otherUser.getUserName() != null
              ? ContactInfo.TelegramContact.builder().username(otherUser.getUserName()).build()
              : null;
      var result =
          linkCodeService.attemptMessengerLinking(text, ContactInfoType.TELEGRAM, channel, contact);
      try {
        switch (result) {
          case LinkingAttempt.Failed ignore -> {
            log.error("Failed to send user verification confirmation!");
            telegramClient.execute(
                SendMessage.builder()
                    .chatId(message.getChatId())
                    .text(
                        "Account linked did not work, maybe the code you sent is too old? Maybe try it with a fresh one from the website!")
                    .build());
          }
          case LinkingAttempt.LinkAndContactOption linkAndContactOption -> {
            RegisteredUser user = userDao.findOrThrow(linkAndContactOption.userId());
            NotificationContext response =
                NotificationContext.builder()
                    .target(
                        NotificationContext.Target.User.builder()
                            .userId(user.id())
                            .displayName(user.getDisplayName())
                            .channel(
                                new NotificationChannel.Telegram(
                                    message.getChatId(), otherUser.getId()))
                            .build())
                    .payload(
                        NotificationContext.Content.UserMessengerLinked.builder()
                            .username(user.getDisplayName())
                            .build())
                    .locale(Optional.ofNullable(user.getPreferredLocale()).orElse(Locale.ENGLISH))
                    .build();
            notificationManager.dispatch(response);
          }
          case LinkingAttempt.Success success -> {
            // todo send info that contact option did not work because username is null
            RegisteredUser user = userDao.findOrThrow(success.userId());
            NotificationContext response =
                NotificationContext.builder()
                    .target(
                        NotificationContext.Target.User.builder()
                            .userId(user.id())
                            .displayName(user.getDisplayName())
                            .channel(
                                new NotificationChannel.Telegram(
                                    message.getChatId(), otherUser.getId()))
                            .build())
                    .payload(
                        NotificationContext.Content.UserMessengerLinked.builder()
                            .username(user.getDisplayName())
                            .build())
                    .locale(Optional.ofNullable(user.getPreferredLocale()).orElse(Locale.ENGLISH))
                    .build();
            notificationManager.dispatch(response);
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

  void setAdapter(TelegramNotificationAdapter adapter) {
    this.adapter = adapter;
    telegramClient = new OkHttpTelegramClient(adapter.getBotToken());
  }
}

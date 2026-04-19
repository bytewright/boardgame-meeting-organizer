package org.bytewright.bgmo.adapter.bot.telegram;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.notification.NotificationContext;
import org.bytewright.bgmo.domain.model.notification.NotificationTargetType;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.domain.service.notification.NotificationTaskExecutor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramNotificationAdapter implements NotificationTaskExecutor, InitializingBean {
  private final TelegramAdapterProperties adapterProperties;
  private final MessageSource messageSource;
  private final TelegramBot telegramBot;
  private final RegisteredUserDao userDao;

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
    Locale targetLocale = context.locale() != null ? context.locale() : Locale.GERMAN;
    String rawMessage =
        messageSource.getMessage(
            context.messageKey(), context.messageArgs().toArray(new Object[0]), targetLocale);
    String formattedMessage = escapeMarkdown(rawMessage);
    SendMessage message = new SendMessage();
    message.setChatId(getChatId(context));
    message.setText(formattedMessage);
    message.setParseMode("MarkdownV2");

    // Example: Adding the "Join" button if a meetupId is present
    if (context.meetupId() != null) {
      var button =
          InlineKeyboardButton.builder()
              .text("Join Meetup 🎲")
              .callbackData("join:" + context.meetupId())
              .build();
      message.setReplyMarkup(InlineKeyboardMarkup.builder().keyboardRow(List.of(button)).build());
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
            user.getContactInfos().stream()
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
  public void afterPropertiesSet() throws Exception {
    telegramBot.setMeetUpJoinRequestHandler(this::handleJoinRequestFromChat);

    TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
    botsApi.registerBot(telegramBot);
    User telegramBotMe = telegramBot.getMe();
    log.info(
        "Telegram bot with username '{}' is ready, listening to chat updates...",
        telegramBotMe.getUserName());
  }

  private void handleJoinRequestFromChat(UUID meetupId, String telegramChatId) {
    // todo create request
  }
}

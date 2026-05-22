package org.bytewright.bgmo.adapter.notification.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.AdapterSettings;
import org.bytewright.bgmo.domain.model.notification.NotificationContext;
import org.bytewright.bgmo.domain.model.notification.NotificationPayload;
import org.bytewright.bgmo.domain.model.notification.NotificationTargetType;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactOption;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.AdapterSettingsDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@ExtendWith(MockitoExtension.class)
class TelegramNotificationAdapterTest {

  @Mock private TelegramAdapterProperties adapterProperties;
  @Mock private AdapterSettingsDao adapterSettingsDao;
  @Mock private TelegramBot telegramBot;
  @Mock private RegisteredUserDao userDao;
  @Mock private JsonMapper objectMapper;
  @Spy private TelegramTemplateService templateService;

  @InjectMocks private TelegramNotificationAdapter adapter;

  @Captor private ArgumentCaptor<SendMessage> sendMessageCaptor;

  @BeforeEach
  void setUp() throws Exception {
    when(adapterProperties.isEnabled()).thenReturn(true);

    AdapterSettings mockAdapterSettings = mock(AdapterSettings.class);
    when(mockAdapterSettings.getAdapterSettings()).thenReturn("{}");
    when(adapterSettingsDao.findByAdapter(any())).thenReturn(mockAdapterSettings);

    TelegramSettings mockSettings = mock(TelegramSettings.class);
    when(mockSettings.isEnabled()).thenReturn(true);
    when(objectMapper.readValue(any(String.class), eq(TelegramSettings.class)))
        .thenReturn(mockSettings);
  }

  @Test
  void testExecute_GroupNotificationWithMeetupId_SendsMessageWithJoinButton() throws Exception {
    // Arrange
    templateService.afterPropertiesSet();
    UUID meetupId = UUID.randomUUID();
    NotificationContext context =
        NotificationContext.builder()
            .notificationTargetType(NotificationTargetType.GROUP)
            .payload(
                NotificationPayload.MeetupCreated.builder()
                    .meetupUrl(URI.create("https://some.url.de/path/to-meetup/").toURL())
                    .meetupId(meetupId)
                    .title("Board Game Night")
                    .build())
            .locale(Locale.ENGLISH)
            .build();

    String groupChatId = "-100123456789";

    when(adapterProperties.getGroupChatId()).thenReturn(groupChatId);

    // Act
    adapter.execute(context);

    // Assert
    verify(telegramBot, times(1)).execute(sendMessageCaptor.capture());
    SendMessage capturedMessage = sendMessageCaptor.getValue();

    assertNotNull(capturedMessage);
    assertEquals(groupChatId, capturedMessage.getChatId());
    assertEquals("MarkdownV2", capturedMessage.getParseMode());

    assertThat(capturedMessage.getText())
        .contains("Board Game Night")
        .contains("https://some\\.url\\.de/path/to\\-meetup/");
  }

  @Test
  void testExecute_DirectNotificationToUser_DispatchesCorrectlyWithoutButtons()
      throws TelegramApiException {
    // Arrange
    UUID userId = UUID.randomUUID();
    NotificationContext context =
        NotificationContext.builder()
            .payload(NotificationPayload.UserApproved.builder().build())
            .notificationTargetType(NotificationTargetType.DIRECT)
            .locale(Locale.GERMAN)
            .userId(userId)
            .build();

    String directChatId = "987654321";
    RegisteredUser mockUser = mock(RegisteredUser.class);
    ContactInfo.TelegramContact telegramContact = mock(ContactInfo.TelegramContact.class);
    ContactOption contactOption = ContactOption.builder().contactInfo(telegramContact).build();
    when(telegramContact.chatId()).thenReturn(directChatId);
    when(mockUser.getContactOptions()).thenReturn(Set.of(contactOption));
    when(userDao.findOrThrow(userId)).thenReturn(mockUser);

    doReturn("Dein Konto ist freigeschaltet\\.")
        .when(templateService)
        .render(eq(Locale.GERMAN), any());

    // Act
    adapter.execute(context);

    // Assert
    verify(telegramBot, times(1)).execute(sendMessageCaptor.capture());
    SendMessage capturedMessage = sendMessageCaptor.getValue();

    assertNotNull(capturedMessage);
    assertEquals(directChatId, capturedMessage.getChatId());
    assertEquals("Dein Konto ist freigeschaltet\\.", capturedMessage.getText());
    assertEquals("MarkdownV2", capturedMessage.getParseMode());
  }
}

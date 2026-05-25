package org.bytewright.bgmo.adapter.notification.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.time.*;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.AdapterSettings;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.notification.NotificationContext;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactOption;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.AdapterSettingsDao;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.domain.service.user.ContactInfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TelegramNotificationAdapterTest {

  @Mock private TelegramAdapterProperties adapterProperties;
  @Mock private ContactInfoService contactInfoService;
  @Mock private AdapterSettingsDao adapterSettingsDao;
  @Mock private TelegramBot telegramBot;
  @Mock private RegisteredUserDao userDao;
  @Mock private MeetupDao meetupDao;
  @Mock private JsonMapper objectMapper;
  @Spy private TelegramContactRenderer contactRenderer;
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
            .target(NotificationContext.Target.Group.builder().build())
            .payload(
                NotificationContext.Content.MeetupCreated.builder()
                    .meetupUrl(URI.create("https://some.url.de/path/to-meetup/").toURL())
                    .meetupId(meetupId)
                    .title("Board Game Night")
                    .build())
            .locale(Locale.ENGLISH)
            .build();

    UUID creatorId = UUID.randomUUID();
    MeetupEvent meetup =
        MeetupEvent.builder()
            .creatorId(creatorId)
            .areaHint("areaHint")
            .description("test meetup for telegram render")
            .eventDate(ZonedDateTime.of(2026, 05, 24, 18, 0, 0, 0, ZoneOffset.UTC))
            .build();
    when(meetupDao.findOrThrow(meetupId)).thenReturn(meetup);
    ContactOption telegramContact =
        ContactOption.builder()
            .id(UUID.randomUUID())
            .contactInfo(ContactInfo.TelegramContact.builder().telegramUsername("foo").build())
            .build();
    RegisteredUser creator =
        RegisteredUser.builder()
            .primaryContactId(telegramContact.getId())
            .contactOptions(Set.of(telegramContact))
            .build();
    when(userDao.findOrThrow(creatorId)).thenReturn(creator);
    when(creator.resolvePrimaryContact()).thenReturn(Optional.of(telegramContact));
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
    ContactInfo.TelegramContact telegramContact =
        ContactInfo.TelegramContact.builder().chatId("987654321").build();

    NotificationContext context =
        NotificationContext.builder()
            .userTarget(mock())
            .payload(NotificationContext.Content.UserApproved.builder().build())
            .locale(Locale.GERMAN)
            .build();

    doReturn("Dein Konto ist freigeschaltet\\.")
        .when(templateService)
        .render(eq(Locale.GERMAN), any());

    // Act
    adapter.execute(context);

    // Assert
    verify(telegramBot, times(1)).execute(sendMessageCaptor.capture());
    SendMessage capturedMessage = sendMessageCaptor.getValue();

    assertNotNull(capturedMessage);
    assertEquals(telegramContact.chatId(), capturedMessage.getChatId());
    assertEquals("Dein Konto ist freigeschaltet\\.", capturedMessage.getText());
    assertEquals("MarkdownV2", capturedMessage.getParseMode());
  }
}

package org.bytewright.bgmo.adapter.notification.discord;

import jakarta.annotation.Nullable;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.bytewright.bgmo.adapter.notification.discord.model.DiscordAdapterData;
import org.bytewright.bgmo.domain.model.JoinRequestPayload;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.domain.model.RequestState;
import org.bytewright.bgmo.domain.model.notification.NotificationContext;
import org.bytewright.bgmo.domain.model.notification.NotificationContext.Content.MeetupCreated;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatcherForums {
  static final String JOIN_BUTTON_PREFIX = "join:";

  private final DiscordDataService discordDataService;
  private final MessageSource messageSource;
  private final ApiManager apiManager;
  private final RegisteredUserDao userDao;
  private final MeetupDao meetupDao;

  public void dispatchToMeetupForumChannels(
      DiscordSettings settings, UUID meetupId, NotificationContext context) {
    for (DiscordSettings.ForumChannel forumChannel : settings.getForumChannels()) {
      ForumChannel channel = apiManager.getJda().getForumChannelById(forumChannel.getChannelId());
      if (channel == null) {
        log.error(
            "Configured Discord forum channel {} not found/visible to bot",
            forumChannel.getChannelId());
        continue;
      }
      Optional<DiscordAdapterData> dataForMeetup =
          discordDataService.findDataForMeetup(forumChannel, meetupId);
      if (dataForMeetup.isEmpty()) {
        createForumThread(forumChannel, channel, context);
        return;
      }
      updateForumThread(forumChannel, channel, settings, meetupId, context, dataForMeetup.get());
    }
  }

  private void createForumThread(
      DiscordSettings.ForumChannel forumChannel,
      ForumChannel channel,
      NotificationContext context) {
    if (context.payload() instanceof MeetupCreated meetupCreated) {
      postMeetupCreated(forumChannel, channel, meetupCreated);
    } else {
      throw new IllegalStateException("Unexpected value: " + context.payload());
    }
  }

  private void postMeetupCreated(
      DiscordSettings.ForumChannel forumChannel,
      ForumChannel channel,
      MeetupCreated meetupCreated) {
    MeetupEvent meetup = meetupDao.findOrThrow(meetupCreated.meetupId());
    Locale locale = Locale.of(forumChannel.getLocale());
    MessageCreateData message = renderMeetupMessage(meetupCreated.meetupUrl(), meetup, locale);
    channel
        .createForumPost(meetup.getTitle(), message)
        .queue(
            forumPost -> {
              long forumPostThreadId = forumPost.getMessage().getIdLong();
              DiscordAdapterData data =
                  discordDataService.createAndPersistNew(
                      forumChannel, forumPostThreadId, meetupCreated.meetupId());
              log.info(
                  "Created new meetup thread {} (forum {}, msgId: {}) for meetup {}",
                  data.getForumPostThreadId(),
                  data.getForumId(),
                  forumPost.getMessage().getIdLong(),
                  meetupCreated.meetupId());
            },
            failure ->
                log.error(
                    "Failed to create Discord forum post for meetup {}",
                    meetupCreated.meetupId(),
                    failure));
  }

  private void updateForumThread(
      DiscordSettings.ForumChannel forumChannel,
      ForumChannel channel,
      DiscordSettings settings,
      UUID meetupId,
      NotificationContext context,
      DiscordAdapterData discordAdapterData) {

    Locale locale = Locale.of(forumChannel.getLocale());
    MeetupEvent meetup = meetupDao.findOrThrow(meetupId);
    ThreadChannel thread =
        apiManager.getJda().getThreadChannelById(discordAdapterData.getForumPostThreadId());
    if (thread == null) {
      log.error(
          "Discord thread for meetup {} not found/visible to bot: {}",
          meetupId,
          discordAdapterData);
      return;
    }
    URL url =
        (context.payload() instanceof NotificationContext.HasMeetUpUrl meetUpUrl)
            ? meetUpUrl.meetupUrl()
            : null;
    thread
        .retrieveStartMessage()
        .queue(
            startMessage -> {
              MessageCreateData rendered = renderMeetupMessage(url, meetup, locale);
              startMessage
                  .editMessageEmbeds(rendered.getEmbeds())
                  .setComponents(rendered.getComponents())
                  .queue(
                      ok -> {},
                      failure ->
                          log.error(
                              "Failed to update Discord forum starter message for meetup {}",
                              meetupId,
                              failure));
            },
            failure ->
                log.error(
                    "Failed to retrieve Discord forum starter message for meetup {}",
                    meetupId,
                    failure));

    postEventNotice(meetup, thread, context, locale);
  }

  /**
   * Posts a short one-line reply describing what just happened, in addition to the starter-message
   * rebuild above.
   */
  private void postEventNotice(
      MeetupEvent meetup, ThreadChannel thread, NotificationContext context, Locale locale) {
    String text =
        switch (context.payload()) {
          case NotificationContext.Content.JoinRequestCreated jr ->
              messageSource.getMessage(
                  "bgmo.adapter.notification.discord.join-request-created",
                  new Object[] {jr.requesterName()},
                  locale);
          case NotificationContext.Content.JoinRequestApproved ignored ->
              null; // maybe the final attendee selection from strategy can be announced but not
          // individual approvals
          // "✅ A join request was approved.";
          case NotificationContext.Content.MeetupCanceled ignored ->
              messageSource.getMessage(
                  "bgmo.adapter.notification.discord.meetup-canceled", null, locale);
          case NotificationContext.Content.MeetupRescheduled resched ->
              messageSource.getMessage(
                  "bgmo.adapter.notification.discord.meetup-rescheduled",
                  new Object[] {
                    resched
                        .newEventDate()
                        .format(DateTimeFormatter.ofPattern("EEE dd.MM.yyyy, HH:mm", locale))
                  },
                  locale);
          default -> null;
        };
    if (text != null) {
      thread
          .sendMessage(text)
          .queue(
              message ->
                  log.info(
                      "Successfully dispatched notification to forum for: {}",
                      meetup.logIdentity()),
              failure ->
                  log.error("Failed to post forum notice for: {}", meetup.logIdentity(), failure));
    }
  }

  private MessageCreateData renderMeetupMessage(
      @Nullable URL meetupUrl, MeetupEvent meetup, Locale locale) {
    String dateLabel =
        messageSource.getMessage("bgmo.adapter.notification.discord.date-title", null, locale);
    String locationLabel =
        messageSource.getMessage("bgmo.adapter.notification.discord.location-title", null, locale);
    String attendeesLabel =
        messageSource.getMessage("bgmo.adapter.notification.discord.attendees-title", null, locale);
    String pendingLabel =
        messageSource.getMessage("bgmo.adapter.notification.discord.pending-title", null, locale);

    String formattedDate =
        meetup.getEventDate().format(DateTimeFormatter.ofPattern("EEE dd.MM.yyyy, HH:mm", locale));
    String dateValue = meetup.isCanceled() ? "~~%s~~".formatted(formattedDate) : formattedDate;

    List<MeetupJoinRequest> requests =
        meetup.getJoinRequests() == null ? List.of() : meetup.getJoinRequests();
    List<String> accepted =
        requests.stream()
            .filter(r -> r.getRequestState() == RequestState.ACCEPTED)
            .map(MeetupJoinRequest::getPayload)
            .map(jrp -> JoinRequestPayload.displayName(userDao, jrp))
            .toList();
    List<String> pending =
        requests.stream()
            .filter(r -> r.getRequestState() == RequestState.OPEN)
            .map(MeetupJoinRequest::getPayload)
            .map(jrp -> JoinRequestPayload.displayName(userDao, jrp))
            .toList();

    RegisteredUser creator = userDao.findOrThrow(meetup.getCreatorId());

    EmbedBuilder embed =
        new EmbedBuilder()
            .setTitle(
                meetup.isCanceled() ? "❌ " + meetup.getTitle() : meetup.getTitle(),
                meetupUrl != null ? meetupUrl.toString() : null)
            .setDescription(meetup.getDescription())
            .addField(locationLabel, meetup.getAreaHint(), true)
            .addField(dateLabel, dateValue, true)
            .addField(attendeesLabel, accepted.isEmpty() ? "—" : String.join("\n", accepted), true)
            .setAuthor(creator.getDisplayName());
    if (!pending.isEmpty()) {
      embed.addField(pendingLabel, String.join("\n", pending), true);
    }
    MessageCreateBuilder messageBuilder =
        new MessageCreateBuilder().setContent("Meetup on " + dateValue).setEmbeds(embed.build());
    if (!meetup.isCanceled()) {
      String label =
          messageSource.getMessage(
              "bgmo.adapter.notification.discord.request-to-join", null, locale);
      messageBuilder.setComponents(
          ActionRow.of(Button.primary(JOIN_BUTTON_PREFIX + meetup.getId(), label)));
    }
    return messageBuilder.build();
  }
}

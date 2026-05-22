package org.bytewright.bgmo.adapter.notification.email_smpt;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.notification.email_smpt.model.EmailSettings;
import org.bytewright.bgmo.adapter.notification.email_smpt.model.RenderedEmail;
import org.bytewright.bgmo.domain.model.AdapterSettings;
import org.bytewright.bgmo.domain.model.notification.NotificationContext;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.model.user.ContactOption;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.AdapterSettingsProvider;
import org.bytewright.bgmo.domain.service.data.AdapterSettingsDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.domain.service.notification.NotificationManager;
import org.bytewright.bgmo.domain.service.notification.NotificationTaskExecutor;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Email adapter: sends plain-text notification emails to users who have a verified {@link
 * ContactInfo.EmailContact} on their account.
 *
 * <p>This adapter handles DIRECT notifications only. There is no concept of a group email list in
 * this application — group announcements are handled exclusively by the Telegram adapter.
 *
 * <p>The adapter is enabled when both the deployment-level flag ({@code app.mail.enabled=true}) and
 * the in-app admin toggle ({@link EmailSettings#isEnabled()}) are true. This two-level gate mirrors
 * the Telegram adapter's approach.
 *
 * <p>Registration with {@link NotificationManager} is handled by the existing core {@code
 * InitializingBean} that collects all {@link NotificationTaskExecutor} beans and registers them —
 * no changes needed there.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationAdapter implements NotificationTaskExecutor, AdapterSettingsProvider {

  private static final String ADAPTER_NAME = "Email-NotificationTaskExecutor-integration";

  private final EmailAdapterProperties adapterProperties;
  private final EmailTemplateService templateService;
  private final AdapterSettingsDao adapterSettingsDao;
  private final RegisteredUserDao userDao;
  private final JavaMailSender mailSender;
  private final JsonMapper objectMapper;

  @Override
  public boolean supports(NotificationContext context) {
    return switch (context.target()) {
      case NotificationContext.Target.Anon anon ->
          anon.contactInfo().type() == ContactInfoType.EMAIL;
      case NotificationContext.Target.User user ->
          user.primaryContactInfo().type() == ContactInfoType.EMAIL;
      case NotificationContext.Target.Group ignored -> false;
    };
  }

  @Override
  public boolean isContactHandlerFor(ContactInfoType type) {
    return type == ContactInfoType.EMAIL;
  }

  @Override
  @Async
  public void execute(NotificationContext context) {
    String messageKey = context.payload().messageKey();
    if (!isEnabled()) {
      log.info("Email integration is disabled, skipping execution of: {}", messageKey);
      return;
    }
    ContactInfo.EmailContact info = getContact(context);
    EmailSettings settings = getSettings();
    Locale locale = context.locale() != null ? context.locale() : Locale.ENGLISH;
    RenderedEmail rendered = templateService.render(locale, context.payload());

    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(
        settings.getSenderDisplayName() + " <" + adapterProperties.getFromAddress() + ">");
    message.setTo(info.email());
    if (settings.getReplyTo() != null && !settings.getReplyTo().isBlank()) {
      message.setReplyTo(settings.getReplyTo());
    }
    message.setSubject(rendered.subject());
    message.setText(rendered.body() + "\n\n---\n" + settings.getFooterText().get(locale));

    try {
      mailSender.send(message);
      log.info("Sent email notification '{}' to {}", messageKey, context.target());
    } catch (MailException e) {
      log.error("Failed to send email notification '{}' to {}", messageKey, context.target(), e);

      // TODO: Bounce handling
      // Most SMTP relay providers can POST webhook events for hard bounces.
      // Rough implementation plan when this becomes relevant:
      //   1. Add a @PostMapping("/adapter/email/webhook/bounce") in this adapter (or a sibling
      //      controller in this package).
      //   2. Validate the provider's HMAC signature; store the webhook secret as a new field in
      //      EmailAdapterProperties (deployment secret, not in-app setting).
      //   3. Extract the bounced address from the provider-specific webhook payload.
      //   4. Look up the matching ContactOption — either a new query on RegisteredUserDao
      //      (findByEmailContact) or via ContactInfoService.
      //   5. Mark that ContactOption as invalid so it stops consuming quota on every notification.
      //      This likely needs a new method on ContactInfoService or RegisteredUserDao.
    }
  }

  private ContactInfo.EmailContact getContact(NotificationContext context) {
    ContactInfo info =
        switch (context.target()) {
          case NotificationContext.Target.Anon anon -> anon.contactInfo();
          case NotificationContext.Target.User user -> user.primaryContactInfo();
          case NotificationContext.Target.Group ignored ->
              throw new IllegalArgumentException("email cant target groups!");
        };
    if (info instanceof ContactInfo.EmailContact emailContact) return emailContact;
    throw new IllegalArgumentException("Context did not contain email contact!");
  }

  public boolean isEnabled() {
    return adapterProperties.isEnabled() && getSettings().isEnabled();
  }

  @Override
  public AdapterInfo getAdapterInfo() {
    return AdapterInfo.builder()
        .stableName(ADAPTER_NAME)
        .description(
            "Email notification integration — sends plain-text emails to users with a linked email address")
        .build();
  }

  @Override
  public ValidationResult isValidSettingsJson(String jsonData) {
    try {
      EmailSettings settings = objectMapper.readValue(jsonData, EmailSettings.class);
      return settings != null ? ValidationResult.VALID : ValidationResult.INVALID;
    } catch (JacksonException e) {
      log.error("Error while validating email adapter settings JSON: {}", e.getMessage());
      return ValidationResult.INVALID;
    }
  }

  @Override
  public String getDefaultSettings() throws JacksonException {
    return objectMapper.writeValueAsString(EmailSettings.builder().build());
  }

  private EmailSettings getSettings() {
    try {
      AdapterSettings adapterSettings = adapterSettingsDao.findByAdapter(getAdapterInfo());
      return objectMapper.readValue(adapterSettings.getAdapterSettings(), EmailSettings.class);
    } catch (Exception e) {
      log.error(
          "Error fetching email adapter settings, falling back to defaults: {}", e.getMessage());
      return EmailSettings.builder().build();
    }
  }

  private String resolveEmailAddress(RegisteredUser user) {
    Map<UUID, ContactInfo.EmailContact> emailContacts =
        user.getContactOptions().stream()
            .filter(contactOption -> contactOption.getType() == ContactInfoType.EMAIL)
            .map(contactOption -> Map.entry(contactOption.getId(), getEmailContact(contactOption)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    UUID primaryId = user.getPrimaryContactId();
    if (emailContacts.containsKey(primaryId)) {
      return emailContacts.get(primaryId).email();
    }
    // todo - how the choose? Maybe only allow one email? most likely a user must be able to select
    //  one specific for each meetup/join request
    return emailContacts.values().stream()
        .findAny()
        .map(ContactInfo.EmailContact::email)
        .orElseThrow();
  }

  private ContactInfo.EmailContact getEmailContact(ContactOption contactOption) {
    if (contactOption.getContactInfo() instanceof ContactInfo.EmailContact emailContact) {
      return emailContact;
    }
    throw new IllegalStateException(
        "Found email type conect but not instance of ContactInfo.EmailContact: "
            + contactOption.getId());
  }
}

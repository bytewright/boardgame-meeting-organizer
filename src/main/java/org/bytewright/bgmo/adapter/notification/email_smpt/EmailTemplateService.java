package org.bytewright.bgmo.adapter.notification.email_smpt;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.notification.email_smpt.model.RenderedEmail;
import org.bytewright.bgmo.domain.model.notification.NotificationContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Loads plain-text email templates from the classpath at startup and caches compiled Handlebars
 * templates in memory.
 *
 * <p>Template files live at {@code src/main/resources/templates/email/<messageKey>.json}.
 *
 * <p>File format:
 *
 * <pre>{@code
 * {
 *   "de": { "subject": "Neues Treffen: {{title}}", "body": "Hallo, ..." },
 *   "en": { "subject": "New meetup: {{title}}",    "body": "Hi, ..."   }
 * }
 * }</pre>
 *
 * <p>The supported locales are exactly {@link Locale#ENGLISH} and {@link Locale#GERMAN}. Passing
 * any other locale to {@link #render} throws {@link IllegalArgumentException} — this is intentional
 * so missing templates are caught early rather than silently falling back.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateService implements InitializingBean {

  private static final List<Locale> SUPPORTED_LOCALES = List.of(Locale.ENGLISH, Locale.GERMAN);
  private final Map<String, Map<Locale, EmailTemplate>> templateCache = new ConcurrentHashMap<>();
  private final Handlebars handlebars = new Handlebars();
  private final JsonMapper objectMapper;

  @Override
  public void afterPropertiesSet() throws Exception {
    List<String> requiredKeys =
        NotificationContext.Content.allMessageKeys().stream()
            .sorted(Comparator.naturalOrder())
            .toList();

    for (String key : requiredKeys) {
      String resourcePath = "/templates/email/" + key + ".json";
      ClassPathResource resource = new ClassPathResource(resourcePath);

      if (!resource.exists()) {
        throw new IllegalStateException(
            "Missing email template file: "
                + resourcePath
                + " — add the file or remove the corresponding NotificationType");
      }

      Map<Locale, EmailTemplate> byLocale = new HashMap<>();
      try (InputStream is = resource.getInputStream()) {
        JsonNode root = objectMapper.readTree(is);

        for (Locale locale : SUPPORTED_LOCALES) {
          JsonNode localeNode = root.get(locale.getLanguage());
          if (localeNode == null) {
            throw new IllegalStateException(
                "Missing locale '" + locale.getLanguage() + "' in email template: " + resourcePath);
          }
          JsonNode subjectNode = localeNode.get("subject");
          JsonNode bodyNode = localeNode.get("body");
          if (subjectNode == null || bodyNode == null) {
            throw new IllegalStateException(
                "Template '"
                    + resourcePath
                    + "' locale '"
                    + locale.getLanguage()
                    + "' must contain both 'subject' and 'body' fields");
          }

          Template subject = handlebars.compileInline(subjectNode.asString());
          Template body = handlebars.compileInline(bodyNode.asString());
          byLocale.put(locale, new EmailTemplate(subject, body));
        }
      } catch (IOException e) {
        throw new IllegalStateException("Failed to parse email template: " + resourcePath, e);
      }

      templateCache.put(key, byLocale);
    }

    log.info(
        "Successfully loaded and verified {} email templates ({} locales each).",
        requiredKeys.size(),
        SUPPORTED_LOCALES.size());
  }

  public RenderedEmail render(Locale locale, NotificationContext.Content payload) {
    if (!payload.isUsingI18N()) {
      // messageKey is used as a literal message
      return new RenderedEmail(payload.messageKey(), payload.messageKey());
    }

    if (!SUPPORTED_LOCALES.contains(locale)) {
      throw new IllegalArgumentException(
          "Unsupported locale for email templates: '%s'. Supported locales: %s"
              .formatted(locale, SUPPORTED_LOCALES));
    }
    Map<Locale, EmailTemplate> byLocale = templateCache.get(payload.messageKey());
    if (byLocale == null) {
      log.error(
          "Email template not found for given message, assuming non-message key: {}",
          payload.messageKey());
      return new RenderedEmail("Notification", "Notification: " + payload.messageKey());
    }

    EmailTemplate template = byLocale.get(locale);
    try {
      return new RenderedEmail(template.subject().apply(payload), template.body().apply(payload));
    } catch (IOException e) {
      log.error("Failed to render email template for key '{}'", payload.messageKey(), e);
      return new RenderedEmail("Notification", "Notification: " + payload.messageKey());
    }
  }

  /**
   * Holds the compiled Handlebars templates for a single notification type + locale combination.
   */
  record EmailTemplate(Template subject, Template body) {}
}

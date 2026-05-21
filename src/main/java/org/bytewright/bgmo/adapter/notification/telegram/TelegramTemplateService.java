package org.bytewright.bgmo.adapter.notification.telegram;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.notification.NotificationPayload;
import org.bytewright.bgmo.domain.model.notification.NotificationType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TelegramTemplateService implements InitializingBean {

  private final Map<Locale, Map<String, Template>> templateCache = new ConcurrentHashMap<>();
  private final Handlebars handlebars;

  public TelegramTemplateService() {
    // We set up a template loader to look into src/main/resources/templates/telegram/
    TemplateLoader loader = new ClassPathTemplateLoader("/templates/telegram", ".hbs");

    this.handlebars =
        new Handlebars(loader)
            .with(
                value -> {
                  if (value == null) return "";
                  // Escapes only the variables injected into the templates for MarkdownV2
                  return value.toString().replaceAll("([_*\\[\\]()~`>#+\\-=|{}.!])", "\\\\$1");
                });
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    List<String> requiredKeys =
        Arrays.stream(NotificationType.values())
            .map(NotificationType::getMessageKey)
            .sorted(Comparator.naturalOrder())
            .toList();
    List<Locale> supportedLocales = List.of(Locale.ENGLISH, Locale.GERMAN);

    for (Locale locale : supportedLocales) {
      Map<String, Template> localeCache = new HashMap<>();
      for (String key : requiredKeys) {
        try {
          String path = locale.getLanguage() + "/" + key;
          Template template = handlebars.compile(path);
          localeCache.put(key, template);
        } catch (IOException e) {
          throw new IllegalStateException(
              "Missing Telegram template for key: '" + key + "' and locale: '" + locale + "'", e);
        }
      }
      templateCache.put(locale, localeCache);
    }
    log.info(
        "Successfully loaded and verified {} Telegram templates.",
        requiredKeys.size() * supportedLocales.size());
  }

  public String render(Locale locale, NotificationPayload payload) {
    if (!payload.isUsingI18N()) {
      // messageKey is the actual message
      return payload.messageKey();
    }
    // Fallback to English if the requested locale isn't cached
    Locale targetLocale = templateCache.containsKey(locale) ? locale : Locale.ENGLISH;
    Template template = templateCache.get(targetLocale).get(payload.messageKey());
    if (template == null) {
      log.error("Telegram template not found for key: {}", payload.messageKey());
      return "Notification: " + payload.messageKey();
    }
    try {
      return template.apply(payload);
    } catch (IOException e) {
      log.error("Failed to render telegram template for key {}", payload.messageKey(), e);
      return "Notification: " + payload.messageKey();
    }
  }
}

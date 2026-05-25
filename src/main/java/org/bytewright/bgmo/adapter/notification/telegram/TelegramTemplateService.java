package org.bytewright.bgmo.adapter.notification.telegram;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.notification.NotificationContext;
import org.bytewright.bgmo.domain.service.JsonMapperFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
public class TelegramTemplateService implements InitializingBean {
  private static final List<Locale> SUPPORTED_LOCALES = List.of(Locale.ENGLISH, Locale.GERMAN);

  private static final String RESOURCE_PATH = "/templates/telegram/";
  private final Map<String, Map<Locale, Template>> templateCache = new ConcurrentHashMap<>();
  private final Handlebars handlebars;

  public TelegramTemplateService() {
    this.handlebars =
        new Handlebars()
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
        NotificationContext.Content.allMessageKeys().stream()
            .sorted(Comparator.naturalOrder())
            .toList();
    JsonMapper jsonMapper = JsonMapperFactory.unRedactedMapper();
    for (String key : requiredKeys) {
      String resourcePath = RESOURCE_PATH + key + ".json";
      ClassPathResource resource = new ClassPathResource(resourcePath);

      if (!resource.exists()) {
        throw new IllegalStateException(
            "Missing email template file: "
                + resourcePath
                + " — add the file or remove the corresponding NotificationType");
      }
      Map<Locale, Template> localeCache = new HashMap<>();
      try (InputStream is = resource.getInputStream()) {
        JsonNode root = jsonMapper.readTree(is);
        for (Locale locale : SUPPORTED_LOCALES) {
          JsonNode localeNode = root.get(locale.getLanguage());
          if (localeNode == null) {
            throw new IllegalStateException(
                "Missing locale '" + locale.getLanguage() + "' in email template: " + resourcePath);
          }
          String msgTemplate = localeNode.asString();
          Template template = handlebars.compileInline(msgTemplate);
          localeCache.put(locale, template);
        }
      }
      templateCache.put(key, localeCache);
    }
    log.info("Successfully loaded and verified {} Telegram templates.", templateCache.size());
  }

  public String render(Locale locale, NotificationContext.Content payload) {
    if (!payload.isUsingI18N()) {
      // messageKey is the actual message
      return payload.messageKey();
    }
    return render(locale, payload.messageKey(), payload);
  }

  public String render(Locale locale, String messageKey, Object vars) {
    Map<Locale, Template> localeTemplateMap = templateCache.get(messageKey);
    if (!localeTemplateMap.containsKey(locale)) {
      log.warn("Failed to find messageKey {} with locale {} in cache!", messageKey, locale);
      return "Notification: " + messageKey;
    }
    Template template = localeTemplateMap.get(locale);
    try {
      return template.apply(vars);
    } catch (IOException e) {
      log.error("Failed to render telegram template for key {}", messageKey, e);
      return "Notification: " + messageKey;
    }
  }
}

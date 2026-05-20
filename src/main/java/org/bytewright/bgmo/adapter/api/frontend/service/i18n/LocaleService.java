package org.bytewright.bgmo.adapter.api.frontend.service.i18n;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.VaadinSession;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.api.frontend.service.SessionInfoService;
import org.bytewright.bgmo.usecases.UserWorkflows;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocaleService {
  private static final DateTimeFormatter DATE_FMT_EN =
      DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy", Locale.ENGLISH);
  private static final DateTimeFormatter DATE_FMT_DE =
      DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy", Locale.GERMAN);
  private static final DateTimeFormatter DATE_TIME_FMT_EN =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.ENGLISH);
  private static final DateTimeFormatter DATE_TIME_FMT_DE =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.GERMAN);

  private final SessionInfoService authService;
  private final MessageSource messageSource;
  private final UserWorkflows userWorkflows;

  public DateTimeFormatter getDateFormatter() {
    Locale userLocale = VaadinSession.getCurrent().getLocale();
    if (userLocale.equals(Locale.GERMAN)) {
      return DATE_FMT_DE;
    }
    return DATE_FMT_EN;
  }

  public DateTimeFormatter getTimeFormatter() {
    Locale userLocale = VaadinSession.getCurrent().getLocale();
    return DateTimeFormatter.ofPattern("HH:mm", userLocale);
  }

  public DateTimeFormatter getDateTimeFormatter() {
    Locale userLocale = VaadinSession.getCurrent().getLocale();
    if (userLocale.equals(Locale.GERMAN)) {
      return DATE_TIME_FMT_DE;
    }
    return DATE_TIME_FMT_EN;
  }

  public List<Locale> getSupportedLocales() {
    return List.of(Locale.GERMAN, Locale.ENGLISH);
  }

  public String getLabel(Locale locale) {
    return locale.equals(Locale.GERMAN) ? "🇩🇪" : "🇬🇧";
  }

  public void changeLocale(Locale newLocale) {
    UI.getCurrent().setLocale(newLocale);
    UI.getCurrent().getSession().setLocale(newLocale);
    authService
        .getCurrentUser()
        .ifPresent(registeredUser -> userWorkflows.changeLocale(registeredUser.getId(), newLocale));
    String message = messageSource.getMessage("profile.status.saved", null, newLocale);
    Notification.show(message);
    UI.getCurrent().refreshCurrentRoute(true);
  }

  /**
   * Returns a human-readable relative timestamp for {@code instant}, e.g. "3 days ago".
   *
   * <p>Uses the current Vaadin session locale and the application's {@link MessageSource} so the
   * string is properly translated. The i18n keys required are listed below.
   *
   * <p>Buckets (all use the absolute value of the duration, so future instants also work):
   *
   * <ul>
   *   <li>&lt; 1 min → {@code time.relative.justNow}
   *   <li>&lt; 1 h → {@code time.relative.minutesAgo} (param: minutes)
   *   <li>&lt; 24 h → {@code time.relative.hoursAgo} (param: hours)
   *   <li>≥ 24 h → {@code time.relative.daysAgo} (param: days)
   * </ul>
   */
  public String formatRelative(Instant instant) {
    Locale locale = VaadinSession.getCurrent().getLocale();
    Duration duration = Duration.between(instant, Instant.now()).abs();
    long minutes = duration.toMinutes();
    if (minutes < 1) {
      return messageSource.getMessage("time.relative.justNow", null, locale);
    }
    long hours = duration.toHours();
    if (hours < 1) {
      return messageSource.getMessage("time.relative.minutesAgo", new Object[] {minutes}, locale);
    }
    long days = duration.toDays();
    if (days < 1) {
      return messageSource.getMessage("time.relative.hoursAgo", new Object[] {hours}, locale);
    }
    return messageSource.getMessage("time.relative.daysAgo", new Object[] {days}, locale);
  }
}

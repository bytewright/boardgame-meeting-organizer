package org.bytewright.bgmo.adapter.api.frontend.service.i18n;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.VaadinSession;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.api.frontend.SessionAuthenticationService;
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

  private final SessionAuthenticationService authService;
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
}

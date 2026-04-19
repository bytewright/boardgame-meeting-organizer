package org.bytewright.bgmo.adapter.api.frontend.service.i18n;

import com.vaadin.flow.server.VaadinSession;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LocaleService {
  // private static final Locale DEFAULT_LOCALE = Locale.GERMAN;
  private static final DateTimeFormatter DATE_FMT_EN =
      DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy", Locale.ENGLISH);
  private static final DateTimeFormatter DATE_FMT_DE =
      DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy", Locale.GERMAN);

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
}

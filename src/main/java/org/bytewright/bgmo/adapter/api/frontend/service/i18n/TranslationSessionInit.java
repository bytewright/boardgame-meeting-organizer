package org.bytewright.bgmo.adapter.api.frontend.service.i18n;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TranslationSessionInit implements VaadinServiceInitListener {
  @Override
  public void serviceInit(ServiceInitEvent event) {
    log.info("Setting german as default for new session");
    event
        .getSource()
        .addSessionInitListener(sessionEvent -> sessionEvent.getSession().setLocale(Locale.GERMAN));
  }
}

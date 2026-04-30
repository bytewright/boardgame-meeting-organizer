package org.bytewright.bgmo.adapter.api.frontend;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_LONG;
import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@StyleSheet(Lumo.STYLESHEET)
@ColorScheme(ColorScheme.Value.DARK)
@Push
@Meta(name = "Author", content = "Bytewright")
@PWA(name = APP_NAME_LONG, shortName = APP_NAME_SHORT)
@Configuration
@Profile("!test")
@RequiredArgsConstructor
public class VaadinContextConfig implements AppShellConfigurator, VaadinServiceInitListener {
  private final GlobalErrorHandler globalErrorHandler;

  @Override
  public void configurePage(AppShellSettings settings) {
    log.info("Adding error handlers to Vaadin");
  }

  @Override
  public void serviceInit(ServiceInitEvent event) {
    event
        .getSource()
        .addSessionInitListener(
            sessionInitEvent -> sessionInitEvent.getSession().setErrorHandler(globalErrorHandler));
  }
}

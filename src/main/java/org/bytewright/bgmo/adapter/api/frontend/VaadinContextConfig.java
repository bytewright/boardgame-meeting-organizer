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
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import com.vaadin.flow.theme.lumo.Lumo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.api.frontend.view.LoginView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

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
    // todo
    // settings.addFavIcon();
  }

  @Override
  public void serviceInit(ServiceInitEvent event) {
    event
        .getSource()
        .addSessionInitListener(
            sessionInitEvent -> sessionInitEvent.getSession().setErrorHandler(globalErrorHandler));
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
        registry -> {
          registry.requestMatchers("/assets/**").permitAll();
        });
    http.with(
        VaadinSecurityConfigurer.vaadin(),
        configurer -> {
          configurer.loginView(LoginView.class, "/").defaultSuccessUrl("/dashboard", true);
        });
    return http.build();
  }
}

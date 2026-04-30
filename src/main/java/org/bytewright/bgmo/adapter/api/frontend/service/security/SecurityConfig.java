package org.bytewright.bgmo.adapter.api.frontend.service.security;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.bytewright.bgmo.adapter.api.frontend.view.LoginView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
// @EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.with(
        VaadinSecurityConfigurer.vaadin(),
        configurer -> {
          configurer.loginView(LoginView.class);
        });

    http.formLogin(cfg -> cfg.successForwardUrl("/dashboard"));

    return http.build();
  }
}

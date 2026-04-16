package org.bytewright.bgmo.adapter.api.frontend.service.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.bytewright.bgmo.adapter.api.frontend.view.LoginView;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

@Component
public class BgmoVaadinWebSecurity extends VaadinWebSecurity {

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    super.configure(http); // Vaadin-specific path handling
    setLoginView(http, LoginView.class, "/");
  }
}

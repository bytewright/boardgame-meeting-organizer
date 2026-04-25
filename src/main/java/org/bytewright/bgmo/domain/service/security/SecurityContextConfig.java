package org.bytewright.bgmo.domain.service.security;

import java.util.HashMap;
import java.util.Map;
import org.bytewright.bgmo.domain.service.BgmoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@EnableWebSecurity
@Configuration
public class SecurityContextConfig {
  public static final String DEFAULT_PW_ENCODER = "argon2@SpringSecurity_v5_8";

  /**
   * @see PasswordEncoderFactories#createDelegatingPasswordEncoder
   */
  @Bean
  public PepperingPasswordEncoder passwordEncoder(BgmoProperties bgmoProperties) {
    Map<String, PasswordEncoder> encoders = new HashMap<>();
    encoders.put("bcrypt", new BCryptPasswordEncoder());
    encoders.put(DEFAULT_PW_ENCODER, Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());
    DelegatingPasswordEncoder encoder = new DelegatingPasswordEncoder(DEFAULT_PW_ENCODER, encoders);
    encoder.setDefaultPasswordEncoderForMatches(encoders.get(DEFAULT_PW_ENCODER));
    return new PepperingPasswordEncoder(encoder, bgmoProperties.getSecurityPwPepper());
  }

  @Bean
  public DaoAuthenticationProvider authenticationProvider(
      BgmoUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(passwordEncoder);
    provider.setUserDetailsService(userDetailsService);
    // by setting UserDetailsPasswordService here, the DaoAuthenticationProvider will automatically
    // upgrade pw encodings should DEFAULT_PW_ENCODER ever change on user login
    provider.setUserDetailsPasswordService(userDetailsService);
    return provider;
  }
}

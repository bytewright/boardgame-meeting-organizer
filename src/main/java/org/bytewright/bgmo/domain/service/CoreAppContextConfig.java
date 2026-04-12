package org.bytewright.bgmo.domain.service;

import java.time.Clock;
import java.time.ZoneOffset;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreAppContextConfig {
  @Bean
  Clock realTimeClock() {
    return Clock.system(ZoneOffset.UTC);
  }
}

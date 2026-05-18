package org.bytewright.bgmo.domain.service;

import java.time.Clock;
import java.time.ZoneOffset;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@Configuration
public class CoreAppContextConfig {
  public static final String APP_NAME_SHORT = "BGMO";
  public static final String APP_NAME_LONG = "Boardgame Meeting Organizer";

  @Bean
  Clock realTimeClock() {
    return Clock.system(ZoneOffset.UTC);
  }
}

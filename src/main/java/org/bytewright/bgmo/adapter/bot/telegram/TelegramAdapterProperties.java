package org.bytewright.bgmo.adapter.bot.telegram;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConfigurationProperties("bgmo.adapter.bot.telegram")
public class TelegramAdapterProperties {
  private String botToken;
  private String botUsername;
  private String groupChatId;
  private String groupLocale;
}

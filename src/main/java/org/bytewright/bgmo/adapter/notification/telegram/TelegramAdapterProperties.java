package org.bytewright.bgmo.adapter.notification.telegram;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("bgmo.adapter.notification.telegram")
public class TelegramAdapterProperties {
  private String botToken;
  private String botUsername;
  private String botDisplayName;
  private String groupChatId;
  private String groupLocale;
  private boolean enabled;
}

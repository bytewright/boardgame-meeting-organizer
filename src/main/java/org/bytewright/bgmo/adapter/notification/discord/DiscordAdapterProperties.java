package org.bytewright.bgmo.adapter.notification.discord;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bgmo.adapter.notification.discord")
public class DiscordAdapterProperties {

  /**
   * Master on/off switch, independent of the runtime-editable {@link DiscordSettings#isEnabled()}.
   */
  private boolean enabled;

  /** Bot token from the Discord Developer Portal's "Bot" tab */
  private String botToken;

  /**
   * The application's Client ID from the Developer Portal's "OAuth2" tab. Needed to build the
   * server-invite URL
   */
  private String clientId;

  private String botDisplayName;
}

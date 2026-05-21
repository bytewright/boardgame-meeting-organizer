package org.bytewright.bgmo.adapter.notification.telegram;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class TelegramSettings {
  @Builder.Default private boolean enabled = true;
  @Builder.Default private String iconLink = "assets/images/poweredByBggLogo.webp";
  @Builder.Default private String tutorialStep1Link = "assets/images/telegram-help-1.jpg";
  @Builder.Default private String tutorialStep2Link = "assets/images/telegram-help-2.jpg";
  @Builder.Default private String tutorialStep3Link = "assets/images/telegram-help-3.jpg";
}

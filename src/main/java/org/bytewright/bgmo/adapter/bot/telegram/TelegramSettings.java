package org.bytewright.bgmo.adapter.bot.telegram;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class TelegramSettings {
  @Builder.Default private boolean enabled = true;
}

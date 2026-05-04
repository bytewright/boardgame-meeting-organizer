package org.bytewright.bgmo.adapter.bgg;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class BggAdapterSettings {
  @Builder.Default private String logoLink = "assets/images/poweredByBggLogo.webp";
  @Builder.Default private String displayName = "BoardGameGeek";
  @Builder.Default private boolean enabled = true;
}

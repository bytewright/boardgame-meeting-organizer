package org.bytewright.bgmo.adapter.bgg;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BggAdapterSettings {
  @Builder.Default private String displayName = "BoardGameGeek";
  @Builder.Default private String logoLink = "path-to-logo.png";
}

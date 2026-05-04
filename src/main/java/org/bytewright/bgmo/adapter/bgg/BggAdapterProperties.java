package org.bytewright.bgmo.adapter.bgg;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;

@Data
@Component
@ConfigurationProperties("bgmo.adapter.bgg")
public class BggAdapterProperties {
  private String apiToken;
  private String bggBaseUrl;
  private boolean enabled;
}

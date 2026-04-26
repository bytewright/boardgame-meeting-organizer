package org.bytewright.bgmo.domain.service;

import java.net.URI;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("bgmo.core.services")
public class BgmoProperties {
  private boolean automationAutostart;
  private String securityPwPepper;
  private URI baseUrl;
  private String profanityFilterListPath;
}

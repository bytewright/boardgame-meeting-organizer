package org.bytewright.bgmo.adapter.notification.email_smpt;

import lombok.Data;
import org.bytewright.bgmo.adapter.notification.email_smpt.model.EmailSettings;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Boot-time configuration for the email adapter. Secrets and deployment-level toggles belong here.
 * Runtime/admin-tunable settings belong in {@link EmailSettings} via AdapterSettingsDao.
 */
@Data
@Component
@ConfigurationProperties(prefix = "bgmo.adapter.notification.email-smpt")
public class EmailAdapterProperties {
  private boolean enabled = false;

  /** The raw from-address used in the SMTP envelope */
  private String fromAddress;
}

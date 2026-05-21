package org.bytewright.bgmo.adapter.notification.email_smpt.model;

import java.util.Locale;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bytewright.bgmo.adapter.notification.email_smpt.EmailAdapterProperties;

/**
 * Admin-tunable email settings stored as JSON in the database via {@link
 * org.bytewright.bgmo.domain.service.data.AdapterSettingsDao}.
 *
 * <p>These are the knobs site admins can turn at runtime without redeploying. Secrets and
 * deployment-level flags belong in {@link EmailAdapterProperties} instead.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailSettings {

  @Builder.Default private boolean enabled = false;

  /** Display name shown in the From header */
  @Builder.Default private String senderDisplayName = "Boardgame meeting organizer";

  /** Optional reply-to address. Leave blank to omit the header — replies will go to fromAddress. */
  @Builder.Default private String replyTo = "";

  /** Plain-text footer appended to every outgoing email. */
  @Builder.Default
  private Map<Locale, String> footerText =
      Map.of(
          Locale.GERMAN,
          "Du bekommst diese E-Mail weil du einen Account auf BGMO hast.\n"
              + "Log doch auf der Webseite ein um deine Benachrichtigungsoptionen zu ändern.",
          Locale.ENGLISH,
          "You are receiving this message because you have an account at BGMO.\n"
              + "Log in to manage your notification preferences.");
}

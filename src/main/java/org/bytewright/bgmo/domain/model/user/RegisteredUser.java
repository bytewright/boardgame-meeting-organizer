package org.bytewright.bgmo.domain.model.user;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.*;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.bytewright.bgmo.domain.model.data.HasUUID;
import org.bytewright.bgmo.domain.model.notification.NotificationChannel;

@Data
@Builder(toBuilder = true)
public class RegisteredUser implements HasUUID {
  private UUID id;
  private String displayName;
  private String loginName;
  @ToString.Exclude private String passwordHash;
  private Instant tsCreation;
  private Instant tsModified;
  private Instant tsLastLogin;
  @Nullable private Locale preferredLocale;
  @Builder.Default private UserStatus status = UserStatus.AFTER_REGISTRATION;
  @Builder.Default private UserRole role = UserRole.USER;
  @Nullable private UUID primaryContactId;
  @ToString.Exclude @Builder.Default private Set<ContactOption> contactOptions = new HashSet<>();
  @Builder.Default private NotificationChannel notificationChannel = new NotificationChannel.None();
  @Builder.Default private Set<UUID> ownedGames = new HashSet<>();

  public String logEntity() {
    return "User['%s';%s]".formatted(getDisplayName(), getId());
  }

  @Data
  @Builder(toBuilder = true)
  public static class Creation {
    private String displayName;
    private String loginName;
    private String password;
    private Locale preferredLocale;
  }

  public Optional<ContactOption> resolvePrimaryContact() {
    return contactOptions.stream()
        .filter(contactOption -> contactOption.id().equals(primaryContactId))
        .findAny();
  }
}

package org.bytewright.bgmo.domain.model.user;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.bytewright.bgmo.domain.model.data.HasUUID;

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
  @Builder.Default private UserStatus status = UserStatus.PENDING_APPROVAL;
  @Builder.Default private UserRole role = UserRole.USER;
  @Nullable private UUID primaryContactId;
  @ToString.Exclude @Builder.Default private Set<ContactInfo> contactInfos = new HashSet<>();
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
    @Nullable private String introHowDidYouHear;
    @Nullable private String introAboutYourself;
    @Nullable private String introWhoInvitedYou;
  }
}

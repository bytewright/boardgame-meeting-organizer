package org.bytewright.bgmo.domain.model.user;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import org.bytewright.bgmo.domain.model.data.HasUUID;

@Data
@Builder(toBuilder = true)
public class RegisteredUser implements HasUUID {
  private UUID id;
  private String displayName;
  private String loginName;
  private String email;
  private String signalHandle;
  private String telegramHandle;
  private String passwordHash;
  private Instant tsCreation;
  private Instant tsModified;
  private Instant tsLastLogin;
  private Set<ContactInfo> contactInfos;
  private Set<UUID> ownedGames;
}

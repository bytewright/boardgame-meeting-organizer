package org.bytewright.bgmo.domain.model.user;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import org.bytewright.bgmo.domain.model.data.HasUUID;

@Data
@Builder(toBuilder = true)
public class RegisteredUser implements HasUUID {
  private UUID id;
  private String name;
  private String email;
  private String passwordHash;
  private Instant tsCreation;
  private Instant tsModified;
  private Instant tsLastLogin;
}

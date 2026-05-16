package org.bytewright.bgmo.domain.model.user;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import org.bytewright.bgmo.domain.model.data.HasUUID;

@Data
@Builder
public class ContactOption implements HasUUID {
  private UUID id;
  private UUID userId;
  private Instant tsCreation;
  private Instant tsModified;
  private ContactInfoType type;
  boolean verified;
  private ContactInfo contactInfo;
}

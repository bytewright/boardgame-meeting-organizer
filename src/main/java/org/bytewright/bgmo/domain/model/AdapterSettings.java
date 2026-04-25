package org.bytewright.bgmo.domain.model;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import org.bytewright.bgmo.domain.model.data.HasUUID;

@Data
@Builder(toBuilder = true)
public final class AdapterSettings implements HasUUID {
  private UUID id;
  private String adapterName;
  private String adapterSettings;
  private Instant tsCreation;
  private Instant tsModified;
}

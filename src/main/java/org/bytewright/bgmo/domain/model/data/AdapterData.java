package org.bytewright.bgmo.domain.model.data;

import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
public abstract class AdapterData implements HasUUID {
  private UUID id;
  private String adapterName;
  private String data;
  private Instant tsCreation;
  private Instant tsModified;

  public abstract String getDataIdentifier();
}

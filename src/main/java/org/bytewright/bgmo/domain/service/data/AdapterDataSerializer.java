package org.bytewright.bgmo.domain.service.data;

import java.time.Instant;
import java.util.UUID;
import org.bytewright.bgmo.domain.model.data.AdapterData;
import org.bytewright.bgmo.domain.model.data.HasUUID;

/**
 * Converts between the generic persistence envelope ({@link AdapterDataRecord}) and a concrete
 * {@link AdapterData} subtype.
 */
public interface AdapterDataSerializer<T extends AdapterData> {

  /** Must match {@code AdapterSettingsProvider.AdapterInfo#stableName()} for this adapter. */
  String getAdapterName();

  T toConcreteType(AdapterDataRecord record);

  AdapterDataRecord fromConcreteType(T model);

  /**
   * Generic persistence envelope for any {@link org.bytewright.bgmo.domain.model.data.AdapterData}
   */
  record AdapterDataRecord(
      UUID id,
      String adapterName,
      String dataIdentifier,
      String data,
      Instant tsCreation,
      Instant tsModified)
      implements HasUUID {}
}

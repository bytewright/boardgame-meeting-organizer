package org.bytewright.bgmo.domain.service.data;

import org.bytewright.bgmo.domain.model.AdapterSettings;
import org.bytewright.bgmo.domain.service.AdapterSettingsProvider;

/**
 * @see AdapterSettingsProvider
 */
public interface AdapterSettingsDao extends ModelDao<AdapterSettings> {
  AdapterSettings findByAdapterName(String adapterName);

  boolean existsByAdapterName(String adapterName);
}

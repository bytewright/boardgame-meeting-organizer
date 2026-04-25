package org.bytewright.bgmo.domain.service.data;

import org.bytewright.bgmo.domain.model.AdapterSettings;

public interface AdapterSettingsDao extends ModelDao<AdapterSettings> {
  AdapterSettings findByAdapterName(String adapterName);

  boolean existsByAdapterName(String adapterName);
}

package org.bytewright.bgmo.domain.service.data;

import java.util.Optional;
import org.bytewright.bgmo.domain.model.data.AdapterData;
import org.bytewright.bgmo.domain.service.AdapterSettingsProvider;
import org.bytewright.bgmo.domain.service.AdapterSettingsProvider.AdapterInfo;

/**
 * @see AdapterSettingsProvider
 */
public interface AdapterDataDao {
  <TYPE extends AdapterData> Optional<TYPE> findByAdapterAndIdentifier(
      AdapterInfo adapterInfo, String dataSpecificIdentifier);

  <TYPE extends AdapterData> TYPE persist(TYPE discordAdapterData);
}

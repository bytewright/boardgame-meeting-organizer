package org.bytewright.bgmo.adapter.persistence.dao.mapstruct;

import jakarta.transaction.Transactional;
import java.util.Optional;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.persistence.dao.BaseEntityMapper;
import org.bytewright.bgmo.adapter.persistence.dao.BaseMapperConfig;
import org.bytewright.bgmo.adapter.persistence.dao.repository.AdapterDataRepository;
import org.bytewright.bgmo.adapter.persistence.entity.AdapterDataEntity;
import org.bytewright.bgmo.adapter.persistence.service.AdapterDataSerializerRegistry;
import org.bytewright.bgmo.domain.model.data.AdapterData;
import org.bytewright.bgmo.domain.service.AdapterSettingsProvider;
import org.bytewright.bgmo.domain.service.data.AdapterDataDao;
import org.bytewright.bgmo.domain.service.data.AdapterDataRawDao;
import org.bytewright.bgmo.domain.service.data.AdapterDataSerializer.AdapterDataRecord;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Transactional
@Mapper(config = BaseMapperConfig.class)
@Setter(onMethod_ = {@Autowired})
public abstract class AdapterDataEntityMapper
    extends BaseEntityMapper<AdapterDataRecord, AdapterDataEntity>
    implements AdapterDataRawDao, AdapterDataDao {
  private AdapterDataSerializerRegistry serializerRegistry;
  private AdapterDataRepository adapterDataRepository;

  @Override
  public abstract void updateEntity(
      @MappingTarget AdapterDataEntity currentEntity, AdapterDataRecord model);

  @InheritInverseConfiguration
  @Override
  public abstract AdapterDataRecord toDto(AdapterDataEntity entity);

  @Override
  public <TYPE extends AdapterData> Optional<TYPE> findByAdapterAndIdentifier(
      AdapterSettingsProvider.AdapterInfo adapterInfo, String dataSpecificIdentifier) {
    return adapterDataRepository
        .findByAdapterNameAndDataIdentifier(adapterInfo.stableName(), dataSpecificIdentifier)
        .map(this::toDto)
        .map(adapterDataRecord -> serializerRegistry.toConcreteType(adapterDataRecord));
  }

  @Override
  public <TYPE extends AdapterData> TYPE persist(TYPE model) {
    AdapterDataRecord adapterDataRecord = serializerRegistry.fromConcreteType(model);
    AdapterDataRecord update = createOrUpdate(adapterDataRecord);
    return serializerRegistry.toConcreteType(update);
  }

  @Override
  protected Class<AdapterDataEntity> getEntityClass() {
    return AdapterDataEntity.class;
  }
}

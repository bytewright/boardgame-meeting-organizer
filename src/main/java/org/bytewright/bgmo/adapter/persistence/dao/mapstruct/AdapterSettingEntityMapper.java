package org.bytewright.bgmo.adapter.persistence.dao.mapstruct;

import jakarta.transaction.Transactional;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.persistence.dao.BaseEntityMapper;
import org.bytewright.bgmo.adapter.persistence.dao.BaseMapperConfig;
import org.bytewright.bgmo.adapter.persistence.dao.repository.AdapterSettingsRepository;
import org.bytewright.bgmo.adapter.persistence.entity.AdapterSettingEntity;
import org.bytewright.bgmo.adapter.persistence.entity.AdapterSettingEntity_;
import org.bytewright.bgmo.domain.model.AdapterSettings;
import org.bytewright.bgmo.domain.service.data.AdapterSettingsDao;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

@Slf4j
@Transactional
@Mapper(config = BaseMapperConfig.class)
@Setter(onMethod_ = {@Autowired})
public abstract class AdapterSettingEntityMapper
    extends BaseEntityMapper<AdapterSettings, AdapterSettingEntity> implements AdapterSettingsDao {
  private AdapterSettingsRepository adapterSettingsRepository;

  @Override
  public abstract void updateEntity(
      @MappingTarget AdapterSettingEntity currentEntity, AdapterSettings model);

  @InheritInverseConfiguration
  @Override
  public abstract AdapterSettings toDto(AdapterSettingEntity entity);

  @Override
  protected Class<AdapterSettingEntity> getEntityClass() {
    return AdapterSettingEntity.class;
  }

  @Override
  public AdapterSettings findByAdapterName(String adapterName) {
    Specification<AdapterSettingEntity> specification =
        Specification.where(adapterNameIs(adapterName));
    return adapterSettingsRepository.findOne(specification).map(this::toDto).orElseThrow();
  }

  @Override
  public boolean existsByAdapterName(String adapterName) {
    Specification<AdapterSettingEntity> specification =
        Specification.where(adapterNameIs(adapterName));
    return adapterSettingsRepository.exists(specification);
  }

  private static Specification<AdapterSettingEntity> adapterNameIs(String adapterName) {
    return (root, query, cb) -> cb.equal(root.get(AdapterSettingEntity_.ADAPTER_NAME), adapterName);
  }
}

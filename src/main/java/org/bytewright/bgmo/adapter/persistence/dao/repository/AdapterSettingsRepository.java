package org.bytewright.bgmo.adapter.persistence.dao.repository;

import java.util.UUID;
import org.bytewright.bgmo.adapter.persistence.entity.AdapterSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AdapterSettingsRepository
    extends JpaRepository<AdapterSettingEntity, UUID>,
        JpaSpecificationExecutor<AdapterSettingEntity> {}

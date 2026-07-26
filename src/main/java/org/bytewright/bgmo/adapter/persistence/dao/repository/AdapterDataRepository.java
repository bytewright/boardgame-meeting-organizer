package org.bytewright.bgmo.adapter.persistence.dao.repository;

import java.util.Optional;
import java.util.UUID;
import org.bytewright.bgmo.adapter.persistence.entity.AdapterDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdapterDataRepository extends JpaRepository<AdapterDataEntity, UUID> {
  Optional<AdapterDataEntity> findByAdapterNameAndDataIdentifier(
      String adapterName, String dataIdentifier);
}

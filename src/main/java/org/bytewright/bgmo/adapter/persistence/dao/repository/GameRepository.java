package org.bytewright.bgmo.adapter.persistence.dao.repository;

import java.util.List;
import java.util.UUID;
import org.bytewright.bgmo.adapter.persistence.entity.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRepository extends JpaRepository<GameEntity, UUID> {
  List<GameEntity> findByOwner_IdOrderByNameAsc(UUID id);
}

package org.bytewright.bgmo.adapter.persistance.dao.repository;

import java.util.Optional;
import java.util.UUID;
import org.bytewright.bgmo.adapter.persistance.entity.user.RegisteredUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegisteredUserRepository extends JpaRepository<RegisteredUserEntity, UUID> {
  Optional<RegisteredUserEntity> findByEmailAndPasswordHash(String email, String passwordHash);
}

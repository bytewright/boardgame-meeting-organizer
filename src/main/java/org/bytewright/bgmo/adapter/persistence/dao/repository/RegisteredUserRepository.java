package org.bytewright.bgmo.adapter.persistence.dao.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.bytewright.bgmo.adapter.persistence.entity.user.RegisteredUserEntity;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.model.user.UserRole;
import org.bytewright.bgmo.domain.model.user.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RegisteredUserRepository
    extends JpaRepository<RegisteredUserEntity, UUID>,
        JpaSpecificationExecutor<RegisteredUserEntity> {

  Optional<RegisteredUserEntity> findByLoginName(String loginName);

  boolean existsByIdAndContactInfos_Type(UUID id, ContactInfoType type);

  Stream<RegisteredUserEntity> findByRoleAndStatus(UserRole role, UserStatus status);
}

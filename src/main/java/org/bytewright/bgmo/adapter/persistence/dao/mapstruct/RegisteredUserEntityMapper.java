package org.bytewright.bgmo.adapter.persistence.dao.mapstruct;

import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.persistence.dao.BaseEntityMapper;
import org.bytewright.bgmo.adapter.persistence.dao.BaseMapperConfig;
import org.bytewright.bgmo.adapter.persistence.dao.repository.RegisteredUserRepository;
import org.bytewright.bgmo.adapter.persistence.entity.user.RegisteredUserEntity;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Transactional
@Mapper(config = BaseMapperConfig.class, uses = ContactInfoEntityMapper.class)
@Setter(onMethod_ = {@Autowired})
public abstract class RegisteredUserEntityMapper
    extends BaseEntityMapper<RegisteredUser, RegisteredUserEntity> implements RegisteredUserDao {
  private RegisteredUserRepository userRepository;

  @Mapping(target = "contactInfos", source = "contactInfos")
  @Override
  public abstract void updateEntity(
      @MappingTarget RegisteredUserEntity currentEntity, RegisteredUser model);

  @InheritInverseConfiguration
  @Override
  public abstract RegisteredUser toDto(RegisteredUserEntity entity);

  @Override
  protected Class<RegisteredUserEntity> getEntityClass() {
    return RegisteredUserEntity.class;
  }

  @Override
  public boolean hasContactOfType(UUID userId, ContactInfoType contactInfoType) {
    return userRepository.existsByIdAndContactInfos_Type(userId, contactInfoType);
  }

  @Override
  public Optional<RegisteredUser> findByLoginName(String loginName) {
    return userRepository.findByLoginName(loginName).map(this::toDto);
  }
}

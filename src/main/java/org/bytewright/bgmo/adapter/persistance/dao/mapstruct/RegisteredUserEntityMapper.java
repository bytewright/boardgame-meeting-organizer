package org.bytewright.bgmo.adapter.persistance.dao.mapstruct;

import jakarta.transaction.Transactional;
import java.util.Optional;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.persistance.dao.BaseEntityMapper;
import org.bytewright.bgmo.adapter.persistance.dao.BaseMapperConfig;
import org.bytewright.bgmo.adapter.persistance.dao.repository.RegisteredUserRepository;
import org.bytewright.bgmo.adapter.persistance.entity.user.RegisteredUserEntity;
import org.bytewright.bgmo.adapter.persistance.entity.user.RegisteredUserEntity_;
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
  private RegisteredUserRepository repository;

  @Mapping(target = RegisteredUserEntity_.CONTACT_INFOS, source = "contactInfos")
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
  public Optional<RegisteredUser> findBy(String email, String password) {
    return repository.findByEmailAndPasswordHash(email, password).map(this::toDto);
  }
}

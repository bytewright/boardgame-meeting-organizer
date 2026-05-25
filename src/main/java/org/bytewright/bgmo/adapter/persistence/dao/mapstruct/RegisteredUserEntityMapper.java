package org.bytewright.bgmo.adapter.persistence.dao.mapstruct;

import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.persistence.dao.BaseEntityMapper;
import org.bytewright.bgmo.adapter.persistence.dao.BaseMapperConfig;
import org.bytewright.bgmo.adapter.persistence.dao.repository.RegisteredUserRepository;
import org.bytewright.bgmo.adapter.persistence.entity.user.RegisteredUserEntity;
import org.bytewright.bgmo.domain.model.notification.NotificationChannel;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.model.user.UserRole;
import org.bytewright.bgmo.domain.model.user.UserStatus;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Transactional
@Mapper(config = BaseMapperConfig.class, uses = ContactInfoEntityMapper.class)
@Setter(onMethod_ = {@Autowired})
public abstract class RegisteredUserEntityMapper
    extends BaseEntityMapper<RegisteredUser, RegisteredUserEntity> implements RegisteredUserDao {
  private RegisteredUserRepository userRepository;
  private JsonMapper mapper;

  @Mapping(target = "contactInfos", source = "contactOptions")
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

  @Override
  public Set<RegisteredUser> findAllActiveByRole(UserRole role) {
    return userRepository
        .findByRoleAndStatus(role, UserStatus.ACTIVE)
        .map(this::toDto)
        .collect(Collectors.toSet());
  }

  protected String serializeTaskPayload(NotificationChannel value) {
    return mapper.writeValueAsString(value);
  }

  protected NotificationChannel deserializePayload(String payload) {
    return mapper.readValue(payload, NotificationChannel.class);
  }
}

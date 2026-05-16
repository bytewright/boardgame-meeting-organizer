package org.bytewright.bgmo.adapter.persistence.dao.mapstruct;

import jakarta.transaction.Transactional;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.persistence.dao.BaseEntityMapper;
import org.bytewright.bgmo.adapter.persistence.dao.BaseMapperConfig;
import org.bytewright.bgmo.adapter.persistence.entity.user.ContactInfoEntity;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactOption;
import org.bytewright.bgmo.domain.service.data.ModelDao;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Transactional
@Mapper(config = BaseMapperConfig.class)
@Setter(onMethod_ = {@Autowired})
public abstract class ContactInfoEntityMapper
    extends BaseEntityMapper<ContactOption, ContactInfoEntity> implements ModelDao<ContactOption> {
  private JsonMapper objectMapper;

  @Mapping(target = "user", source = "userId")
  @Mapping(target = "jsonData", source = "contactInfo")
  @Override
  public abstract void updateEntity(
      @MappingTarget ContactInfoEntity currentEntity, ContactOption model);

  @InheritInverseConfiguration
  @Override
  public abstract ContactOption toDto(ContactInfoEntity entity);

  protected String serializeTaskPayload(ContactInfo value) {
    return objectMapper.writeValueAsString(value);
  }

  protected ContactInfo deserializePayload(String payload) {
    return objectMapper.readValue(payload, ContactInfo.class);
  }

  @Override
  protected Class<ContactInfoEntity> getEntityClass() {
    return ContactInfoEntity.class;
  }
}

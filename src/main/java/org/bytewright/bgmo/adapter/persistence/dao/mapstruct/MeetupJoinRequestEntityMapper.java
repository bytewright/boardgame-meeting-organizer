package org.bytewright.bgmo.adapter.persistence.dao.mapstruct;

import jakarta.transaction.Transactional;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.persistence.dao.BaseEntityMapper;
import org.bytewright.bgmo.adapter.persistence.dao.BaseMapperConfig;
import org.bytewright.bgmo.adapter.persistence.entity.meetup.MeetupJoinRequestEntity;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.domain.service.data.ModelDao;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Transactional
@Mapper(config = BaseMapperConfig.class)
@Setter(onMethod_ = {@Autowired})
public abstract class MeetupJoinRequestEntityMapper
    extends BaseEntityMapper<MeetupJoinRequest, MeetupJoinRequestEntity>
    implements ModelDao<MeetupJoinRequest> {

  @Mapping(target = "meetup", source = "meetupId")
  @Mapping(target = "user", source = "userId")
  @Override
  public abstract void updateEntity(
      @MappingTarget MeetupJoinRequestEntity currentEntity, MeetupJoinRequest model);

  @InheritInverseConfiguration
  @Override
  public abstract MeetupJoinRequest toDto(MeetupJoinRequestEntity entity);

  @Override
  protected Class<MeetupJoinRequestEntity> getEntityClass() {
    return MeetupJoinRequestEntity.class;
  }
}

package org.bytewright.bgmo.adapter.persistance.dao.mapstruct;

import jakarta.transaction.Transactional;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.persistance.dao.BaseEntityMapper;
import org.bytewright.bgmo.adapter.persistance.dao.BaseMapperConfig;
import org.bytewright.bgmo.adapter.persistance.entity.meetup.MeetupEntity;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Transactional
@Mapper(config = BaseMapperConfig.class, uses = MeetupJoinRequestEntityMapper.class)
@Setter(onMethod_ = {@Autowired})
public abstract class MeetupEntityMapper extends BaseEntityMapper<MeetupEvent, MeetupEntity>
    implements MeetupDao {

  @Mapping(source = "creatorId", target = "creator.id")
  @Mapping(source = "offeredGames", target = "offeredGames")
  @Override
  public abstract void updateEntity(@MappingTarget MeetupEntity currentEntity, MeetupEvent model);

  @InheritInverseConfiguration
  @Override
  public abstract MeetupEvent toDto(MeetupEntity entity);

  @Override
  protected Class<MeetupEntity> getEntityClass() {
    return MeetupEntity.class;
  }
}

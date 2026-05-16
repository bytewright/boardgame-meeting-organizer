package org.bytewright.bgmo.adapter.persistence.dao.mapstruct;

import jakarta.transaction.Transactional;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.persistence.dao.BaseEntityMapper;
import org.bytewright.bgmo.adapter.persistence.dao.BaseMapperConfig;
import org.bytewright.bgmo.adapter.persistence.dao.repository.MeetupRepository;
import org.bytewright.bgmo.adapter.persistence.entity.meetup.MeetupEntity;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.MeetupEventLocation;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Transactional
@Mapper(config = BaseMapperConfig.class, uses = MeetupJoinRequestEntityMapper.class)
@Setter(onMethod_ = {@Autowired})
public abstract class MeetupEntityMapper extends BaseEntityMapper<MeetupEvent, MeetupEntity>
    implements MeetupDao {
  private MeetupRepository repository;

  @Mapping(source = "creatorId", target = "creator")
  @Mapping(source = "offeredGames", target = "offeredGames")
  @Mapping(source = "registrationClosing", target = "registrationClosing")
  @Override
  public abstract void updateEntity(@MappingTarget MeetupEntity currentEntity, MeetupEvent model);

  @InheritInverseConfiguration
  @Override
  public abstract MeetupEvent toDto(MeetupEntity entity);

  @Override
  protected Class<MeetupEntity> getEntityClass() {
    return MeetupEntity.class;
  }

  @Override
  public Set<MeetupEventLocation> findAllLocationsByOrganizer(UUID userId) {
    return repository
        .findByCreator_Id(userId)
        .map(
            meetupEntity ->
                new MeetupEventLocation(meetupEntity.getAreaHint(), meetupEntity.getFullLocation()))
        .collect(Collectors.toSet());
  }

  @Override
  public Stream<MeetupEvent> findNotExpired(ZonedDateTime now) {
    return repository.findByEventDateAfterAndCanceledFalseOrderByEventDateAsc(now).map(this::toDto);
  }
}

package org.bytewright.bgmo.adapter.persistance.dao.mapstruct;

import jakarta.transaction.Transactional;
import java.util.Optional;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.persistance.dao.BaseEntityMapper;
import org.bytewright.bgmo.adapter.persistance.dao.BaseMapperConfig;
import org.bytewright.bgmo.adapter.persistance.dao.repository.MeetupJoinRequestRepository;
import org.bytewright.bgmo.adapter.persistance.dao.repository.MeetupRepository;
import org.bytewright.bgmo.adapter.persistance.dao.repository.RegisteredUserRepository;
import org.bytewright.bgmo.adapter.persistance.entity.meetup.MeetupEntity;
import org.bytewright.bgmo.adapter.persistance.entity.meetup.MeetupJoinRequestEntity;
import org.bytewright.bgmo.adapter.persistance.entity.meetup.MeetupJoinRequestEntity.MeetupJoinKey;
import org.bytewright.bgmo.adapter.persistance.entity.meetup.MeetupJoinRequestEntity_;
import org.bytewright.bgmo.adapter.persistance.entity.user.RegisteredUserEntity;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Transactional
@Mapper(config = BaseMapperConfig.class)
@Setter(onMethod_ = {@Autowired})
public abstract class MeetupEntityMapper extends BaseEntityMapper<MeetupEvent, MeetupEntity>
    implements MeetupDao {
  private MeetupJoinRequestRepository joinRequestRepository;
  private RegisteredUserRepository userRepository;
  private MeetupRepository meetupRepository;

  @Mapping(source = "creatorId", target = "creator.id")
  @Mapping(source = "confirmedAttendeeIds", target = "confirmedAttendees")
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

  protected MeetupJoinRequestEntity meetupJoinRequestToDb(MeetupJoinRequest request) {
    var compositeKey = new MeetupJoinKey(request.getMeetupId(), request.getUserId());
    Optional<MeetupJoinRequestEntity> requestEntity = joinRequestRepository.findById(compositeKey);
    if (requestEntity.isPresent()) {
      return requestEntity.orElseThrow();
    }
    log.debug("Creating new MeetupJoinRequestEntity: {}", compositeKey);
    MeetupEntity meetup = meetupRepository.getReferenceById(request.getMeetupId());
    RegisteredUserEntity user = userRepository.getReferenceById(request.getUserId());

    return MeetupJoinRequestEntity.builder()
        .meetupJoinKey(compositeKey)
        .meetup(meetup)
        .user(user)
        .tsCreation(request.getTsCreation())
        .build();
  }

  @Mapping(target = "userId", source = "meetupJoinKey.userId")
  @Mapping(target = "meetupId", source = "meetupJoinKey.meetupId")
  @BeanMapping(
      ignoreUnmappedSourceProperties = {
        MeetupJoinRequestEntity_.MEETUP,
        MeetupJoinRequestEntity_.USER
      })
  protected abstract MeetupJoinRequest meetupJoinRequestFromDb(
      MeetupJoinRequestEntity meetupJoinRequestEntity);
}

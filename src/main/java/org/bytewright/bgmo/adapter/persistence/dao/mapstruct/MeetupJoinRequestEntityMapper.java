package org.bytewright.bgmo.adapter.persistence.dao.mapstruct;

import jakarta.transaction.Transactional;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.persistence.dao.BaseEntityMapper;
import org.bytewright.bgmo.adapter.persistence.dao.BaseMapperConfig;
import org.bytewright.bgmo.adapter.persistence.entity.meetup.MeetupJoinRequestEntity;
import org.bytewright.bgmo.adapter.persistence.entity.user.RegisteredUserEntity;
import org.bytewright.bgmo.domain.model.JoinRequestPayload;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.domain.service.data.ModelDao;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Transactional
@Mapper(config = BaseMapperConfig.class)
@Setter(onMethod_ = {@Autowired})
public abstract class MeetupJoinRequestEntityMapper
    extends BaseEntityMapper<MeetupJoinRequest, MeetupJoinRequestEntity>
    implements ModelDao<MeetupJoinRequest> {
  private JsonMapper mapper;

  @Mapping(target = "meetup", source = "meetupId")
  @Mapping(target = "user", source = "payload")
  @Override
  public abstract void updateEntity(
      @MappingTarget MeetupJoinRequestEntity currentEntity, MeetupJoinRequest model);

  @Mapping(target = "meetupId", source = "meetup.id")
  @BeanMapping(ignoreUnmappedSourceProperties = {"user"})
  @Override
  public abstract MeetupJoinRequest toDto(MeetupJoinRequestEntity entity);

  protected RegisteredUserEntity extractUser(JoinRequestPayload payload) {
    return switch (payload) {
      case JoinRequestPayload.Anon ignored -> null;
      case JoinRequestPayload.AnonEmail ignored -> null;
      case JoinRequestPayload.User user ->
          entityManager.getReference(RegisteredUserEntity.class, user.userId());
    };
  }

  protected String serializeTaskPayload(JoinRequestPayload value) {
    return mapper.writeValueAsString(value);
  }

  protected JoinRequestPayload deserializePayload(String payload) {
    return mapper.readValue(payload, JoinRequestPayload.class);
  }

  @Override
  protected Class<MeetupJoinRequestEntity> getEntityClass() {
    return MeetupJoinRequestEntity.class;
  }
}

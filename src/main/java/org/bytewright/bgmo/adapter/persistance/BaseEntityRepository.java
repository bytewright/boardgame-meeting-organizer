package org.bytewright.bgmo.adapter.persistance;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bytewright.bgmo.adapter.persistance.entity.GameEntity;
import org.bytewright.bgmo.adapter.persistance.entity.meetup.MeetupEntity;
import org.bytewright.bgmo.adapter.persistance.entity.user.RegisteredUserEntity;
import org.bytewright.bgmo.domain.model.data.HasUUID;
import org.springframework.stereotype.Component;

@Component
@Transactional(Transactional.TxType.MANDATORY)
@RequiredArgsConstructor
public class BaseEntityRepository {
  private final EntityManager entityManager;

  public RegisteredUserEntity findRegisteredUserEntityById(UUID uuid) {
    return findNullsafe(RegisteredUserEntity.class, uuid);
  }

  public MeetupEntity findMeetupEntityById(UUID uuid) {
    return findNullsafe(MeetupEntity.class, uuid);
  }

  public GameEntity findGameEntityById(UUID uuid) {
    return findNullsafe(GameEntity.class, uuid);
  }

  // todo I don't think this works with mapstruct
  public <T extends HasUUID> T getReferenceById(Class<T> type, UUID id) {
    return entityManager.getReference(type, id);
  }

  public UUID mapEntityToId(HasUUID hasUUID) {
    return hasUUID.getId();
  }

  @Nullable
  private <T extends HasUUID> T findNullsafe(Class<T> aClass, UUID id) {
    if (id == null) {
      return null;
    }
    return entityManager.find(aClass, id);
  }
}

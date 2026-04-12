package org.bytewright.bgmo.adapter.persistance.dao.mapstruct;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.persistance.dao.BaseEntityMapper;
import org.bytewright.bgmo.adapter.persistance.dao.BaseMapperConfig;
import org.bytewright.bgmo.adapter.persistance.dao.repository.GameRepository;
import org.bytewright.bgmo.adapter.persistance.entity.GameEntity;
import org.bytewright.bgmo.domain.model.Game;
import org.bytewright.bgmo.domain.service.data.GameDao;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Transactional
@Mapper(config = BaseMapperConfig.class)
@Setter(onMethod_ = {@Autowired})
public abstract class GameEntityMapper extends BaseEntityMapper<Game, GameEntity>
    implements GameDao {
  private GameRepository gameRepository;

  @Mapping(target = "owner.id", source = "ownerId")
  @Override
  public abstract void updateEntity(@MappingTarget GameEntity currentEntity, Game model);

  @InheritInverseConfiguration
  @Override
  public abstract Game toDto(GameEntity entity);

  @Override
  public List<Game> findGamesByUser(UUID userId) {
    return gameRepository.findByOwner_IdOrderByNameAsc(userId).stream().map(this::toDto).toList();
  }

  @Override
  protected Class<GameEntity> getEntityClass() {
    return GameEntity.class;
  }
}

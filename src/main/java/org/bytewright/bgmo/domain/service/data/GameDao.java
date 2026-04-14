package org.bytewright.bgmo.domain.service.data;

import java.util.List;
import java.util.UUID;
import org.bytewright.bgmo.domain.model.Game;

public interface GameDao extends ModelDao<Game> {
  List<Game> findByOwnerId(UUID userId);
}

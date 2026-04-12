package org.bytewright.bgmo.domain.service.data;

import jakarta.transaction.Transactional;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bytewright.bgmo.domain.model.data.HasUUID;

@Transactional
public interface ModelDao<MODEL_TYPE extends HasUUID> {
  Collection<MODEL_TYPE> findAll();

  Optional<MODEL_TYPE> find(UUID id);

  default List<MODEL_TYPE> findAllById(List<UUID> idList) {
    return idList.stream().map(this::findOrThrow).toList();
  }

  MODEL_TYPE createOrUpdate(MODEL_TYPE model);

  default void delete(MODEL_TYPE model) {
    delete(model.getId());
  }

  void delete(UUID model);

  default MODEL_TYPE findOrThrow(UUID modelId) {
    return find(modelId).orElseThrow();
  }

  boolean exists(UUID modelId);

  default Optional<MODEL_TYPE> findById(UUID uuid) {
    return find(uuid);
  }
}

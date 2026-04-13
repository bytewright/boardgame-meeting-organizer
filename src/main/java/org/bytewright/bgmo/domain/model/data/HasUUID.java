package org.bytewright.bgmo.domain.model.data;

import java.util.UUID;

public interface HasUUID extends HasId<UUID> {
  @Override
  default UUID getId() {
    return id();
  }

  default UUID id() {
    return getId();
  }
}

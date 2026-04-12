package org.bytewright.bgmo.adapter.persistance.entity;

import jakarta.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.bytewright.bgmo.adapter.persistance.EntityUtil;
import org.bytewright.bgmo.domain.model.data.HasId;

public abstract class AbstractEntity<ID_TYPE> implements HasId<ID_TYPE> {

  public abstract ID_TYPE getId();

  protected Object[] getFieldsForEquals() {
    return List.of(Objects.requireNonNull(getId())).toArray();
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    Class<?> oEffectiveClass = EntityUtil.getClassWithoutProxy(o);
    Class<?> thisEffectiveClass = EntityUtil.getClassWithoutProxy(this);
    if (thisEffectiveClass != oEffectiveClass) return false;
    return isEqualEntity(o);
  }

  protected boolean isEqualEntity(@Nonnull Object that) {
    if (that instanceof AbstractEntity<?> entity) {
      if (this.getId() == null || entity.getId() == null) {
        return false;
      }
      return Arrays.equals(getFieldsForEquals(), entity.getFieldsForEquals());
    }
    return false;
  }

  @Override
  public final int hashCode() {
    return Objects.hash(EntityUtil.getClassWithoutProxy(this).hashCode(), getId());
  }
}

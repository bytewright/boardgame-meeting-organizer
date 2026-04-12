package org.bytewright.bgmo.domain.service.data;

import java.util.Optional;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;

public interface RegisteredUserDao extends ModelDao<RegisteredUser> {
  Optional<RegisteredUser> findBy(String email, String password);
}

package org.bytewright.bgmo.domain.service.user;

import java.util.Optional;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;

public interface CurrentUserAccessor {
  Optional<RegisteredUser> getCurrentUser();
}

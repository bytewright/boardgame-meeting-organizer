package org.bytewright.bgmo.domain.service.data;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.model.user.UserRole;

public interface RegisteredUserDao extends ModelDao<RegisteredUser> {
  boolean hasContactOfType(UUID userId, ContactInfoType contactInfoType);

  Optional<RegisteredUser> findByLoginName(String loginName);

  Set<RegisteredUser> findAllActiveByRole(UserRole role);
}

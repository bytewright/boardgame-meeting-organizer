package org.bytewright.bgmo.domain.service.data;

import java.util.UUID;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;

public interface RegisteredUserDao extends ModelDao<RegisteredUser> {
  boolean hasContactOfType(UUID userId, ContactInfoType contactInfoType);
}

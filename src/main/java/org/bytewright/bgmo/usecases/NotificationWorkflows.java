package org.bytewright.bgmo.usecases;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationWorkflows {
  private final RegisteredUserDao userDao;

  /**
   * Marks a user contact info as verified for a specific ContactInfoType
   *
   * @return
   */
  public boolean verifyContactInfo(UUID userId, ContactInfoType type, String chatId) {
    return false;
  }
}

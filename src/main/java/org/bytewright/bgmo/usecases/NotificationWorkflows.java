package org.bytewright.bgmo.usecases;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.ModelDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationWorkflows {
  private final RegisteredUserDao userDao;
  private final ModelDao<ContactInfo> contactInfoDao;

  /**
   * Marks a user contact info as verified for a specific ContactInfoType
   *
   * @return
   */
  public boolean verifyContactInfo(UUID userId, ContactInfoType type, String chatId) {
    Optional<ContactInfo> contactInfoToVerify =
        userDao.find(userId).map(RegisteredUser::getContactInfos).stream()
            .flatMap(Collection::stream)
            .filter(contactInfo -> contactInfo.type() == type)
            .findAny();
    if (contactInfoToVerify.isPresent()) {
      ContactInfo contactInfo = contactInfoToVerify.get();
      ContactInfo verifiedContact =
          switch (contactInfo) {
            case ContactInfo.AddressContact addressContact -> addressContact; // no verification
            case ContactInfo.PhoneContact phoneContact -> phoneContact; // no verification
            case ContactInfo.EmailContact emailContact ->
                emailContact.toBuilder().isVerified(true).build();
            case ContactInfo.SignalContact signalContact ->
                signalContact.toBuilder().isVerified(true).build();
            case ContactInfo.TelegramContact telegramContact ->
                telegramContact.toBuilder().isVerified(true).build();
          };
      contactInfoDao.createOrUpdate(verifiedContact);
      return true;
    }
    return false;
  }
}

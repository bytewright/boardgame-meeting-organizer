package org.bytewright.bgmo.usecases;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
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
  public UUID verifyContactInfo(UUID userId, ContactInfoType type, String chatId) {
    Optional<ContactInfo> contactInfoToVerify =
        userDao.find(userId).map(RegisteredUser::getContactInfos).stream()
            .flatMap(Collection::stream)
            .filter(contactInfo -> contactInfo.type() == type)
            .findAny();
    if (contactInfoToVerify.isPresent()) {
      ContactInfo contactInfo = contactInfoToVerify.get();
      log.info(
          "Verifying exiting contactInfo of type {} for user {}, id: {}",
          type,
          userId,
          contactInfo.id());
      ContactInfo verifiedContact =
          switch (contactInfo) {
            case ContactInfo.AddressContact addressContact -> addressContact; // no verification
            case ContactInfo.PhoneContact phoneContact -> phoneContact; // no verification
            case ContactInfo.EmailContact emailContact ->
                emailContact.toBuilder().isVerified(true).build();
            case ContactInfo.SignalContact signalContact ->
                signalContact.toBuilder().isVerified(true).build();
            case ContactInfo.TelegramContact telegramContact -> {
              var newVerifiedContact = telegramContact.toBuilder().isVerified(true);
              if (!telegramContact.chatId().equals(chatId)) {
                newVerifiedContact.chatId(chatId);
              }
              yield newVerifiedContact.build();
            }
          };
      return contactInfoDao.createOrUpdate(verifiedContact).getId();
    } else {
      log.info(
          "Received valid token for type {} for user {}, creating new contact info...",
          type,
          userId);
      ContactInfo newContactInfo =
          switch (type) {
            case EMAIL -> ContactInfo.EmailContact.builder().userId(userId).email(chatId).build();
            case TELEGRAM ->
                ContactInfo.TelegramContact.builder().userId(userId).chatId(chatId).build();
            case SIGNAL ->
                ContactInfo.SignalContact.builder().userId(userId).signalHandle(chatId).build();
            default ->
                throw new NotImplementedException(
                    "Type %s can't be verified! User: %s".formatted(type, userId));
          };
      return contactInfoDao.createOrUpdate(newContactInfo).getId();
    }
  }
}

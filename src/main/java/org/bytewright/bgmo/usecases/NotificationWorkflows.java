package org.bytewright.bgmo.usecases;

import jakarta.transaction.Transactional;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.model.user.ContactOption;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.ModelDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationWorkflows {
  private final ModelDao<ContactOption> contactInfoDao;
  private final UserWorkflows userWorkflows;
  private final RegisteredUserDao userDao;

  public UUID verifyMessengerContact(
      UUID userId, ContactInfoType type, String chatId, String userName) {
    Optional<ContactOption> contactInfoToVerify =
        userDao.find(userId).map(RegisteredUser::getContactOptions).stream()
            .flatMap(Collection::stream)
            .filter(contact -> contact.getType() == type)
            .findAny();
    if (contactInfoToVerify.isPresent()
        && contactInfoToVerify.get().getType().isSingletonContact()) {
      log.info("User reverified his account...?");
      return contactInfoToVerify.get().id();
    } else {
      return createNewVerifiedSingletonContact(userId, type, chatId, userName);
    }
  }

  private UUID createNewVerifiedSingletonContact(
      UUID userId, ContactInfoType type, String chatId, String userName) {
    log.info("Creating new verified messenger contact info of type {} for userId {}", type, userId);
    ContactInfo newContactInfo =
        switch (type) {
          case TELEGRAM ->
              ContactInfo.TelegramContact.builder()
                  .chatId(chatId)
                  .telegramUsername(userName)
                  .build();
          case SIGNAL -> ContactInfo.SignalContact.builder().signalHandle(chatId).build();
          default ->
              throw new IllegalArgumentException(
                  "Type %s can't be verified! User: %s".formatted(type, userId));
        };
    var persisted = userWorkflows.addContactInfo(userId, newContactInfo, true);
    return persisted.getValue().getId();
  }
}

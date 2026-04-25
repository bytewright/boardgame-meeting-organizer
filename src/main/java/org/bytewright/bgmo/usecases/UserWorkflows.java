package org.bytewright.bgmo.usecases;

import jakarta.transaction.Transactional;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.Game;
import org.bytewright.bgmo.domain.model.data.HasUUID;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.automation.TimeSource;
import org.bytewright.bgmo.domain.service.data.GameDao;
import org.bytewright.bgmo.domain.service.data.ModelDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.domain.service.notification.NotificationManager;
import org.bytewright.bgmo.domain.service.security.BgmoUserDetailsService;
import org.bytewright.bgmo.domain.service.security.PasswordRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserWorkflows {
  private final BgmoUserDetailsService userDetailsService;
  private final ModelDao<ContactInfo> contactInfoModelDao;
  private final NotificationManager notificationManager;
  private final RegisteredUserDao userDao;
  private final TimeSource timeSource;
  private final GameDao gameDao;

  /**
   * User obj should have no id. For updating an existing model use {@link
   * RegisteredUserDao#createOrUpdate(HasUUID)}
   */
  public RegisteredUser create(RegisteredUser.Creation userDto) {
    RegisteredUser newUser =
        userDao.createOrUpdate(
            RegisteredUser.builder()
                .displayName(userDto.getDisplayName())
                .loginName(userDto.getLoginName())
                .passwordHash(userDetailsService.hashPw(userDto.getPassword()))
                .build());
    Set<ContactInfo> contactInfos = newUser.getContactInfos();
    Optional.ofNullable(userDto.getSignalHandle())
        .map(
            s ->
                ContactInfo.SignalContact.builder().signalHandle(s).userId(newUser.getId()).build())
        .ifPresent(contactInfos::add);
    Optional.ofNullable(userDto.getTelegramHandle())
        .map(s -> ContactInfo.TelegramContact.builder().chatId(s).userId(newUser.getId()).build())
        .ifPresent(contactInfos::add);
    Optional.ofNullable(userDto.getEmail())
        .map(s -> ContactInfo.EmailContact.builder().email(s).userId(newUser.getId()).build())
        .ifPresent(contactInfos::add);
    if (contactInfos.isEmpty()) {
      throw new IllegalArgumentException("Users must provide at least one form of contact info!");
    }
    RegisteredUser refetchedUser = userDao.createOrUpdate(newUser);
    log.info("Created user with id {}: {}", refetchedUser.getId(), refetchedUser);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            notificationManager.addUserRegistrationTask(refetchedUser.getId());
          }
        });
    return refetchedUser;
  }

  /**
   * Game obj should have no id. For updating an existing model use {@link
   * GameDao#createOrUpdate(HasUUID)}
   */
  public Game addGameToLibrary(UUID userId, Game game) {
    if (game.getId() != null) return game;
    game.setOwnerId(userId);
    log.info("Adding game to user {}: {}", userId, game);
    return gameDao.createOrUpdate(game);
  }

  public RegisteredUser addContactInfo(UUID userId, ContactInfo contactInfo) {
    ContactInfo contactInfoWithId =
        contactInfoModelDao.createOrUpdate(contactInfo.withUserId(userId));
    RegisteredUser user = userDao.findOrThrow(userId);
    Set<ContactInfo> contactInfos = user.getContactInfos();
    if (contactInfos.stream().allMatch(ci -> ci.getId().equals(contactInfoWithId.id()))) {
      user.setPrimaryContactId(contactInfo.id());
    }
    return userDao.createOrUpdate(user);
  }

  public void removeGameFromLibrary(UUID gameId) {
    // todo this will also remove the game from all meetups, maybe instead remove owner and set all
    // fields to "deleted"?
    gameDao.delete(gameId);
  }

  public void recordLogin(UUID userId) {
    Optional<RegisteredUser> userOptional = userDao.findById(userId);
    if (userOptional.isPresent()) {
      RegisteredUser user = userOptional.get();
      log.info("User {} has logged in", user.getId());
      user.setTsLastLogin(timeSource.now());
      userDao.createOrUpdate(user);
    }
  }

  public void changeDisplayName(UUID userId, String newDisplayName) {
    RegisteredUser user = userDao.findById(userId).orElseThrow();
    if (user.getDisplayName().equals(newDisplayName)) return;
    log.info("User {} changes his display name to: {}", user.logEntity(), newDisplayName);
    user.setDisplayName(newDisplayName);
    userDao.createOrUpdate(user);
    // TODO profanity filter
  }

  public void changePassword(UUID userId, String newPassword) {
    RegisteredUser user = userDao.findById(userId).orElseThrow();
    if (newPassword.length() < PasswordRules.PW_MIN_CHARS
        || newPassword.length() > PasswordRules.PW_MAX_CHARS) {
      throw new IllegalArgumentException(
          "New PW for user %s violates password rules".formatted(userId));
    }
    userDetailsService.updatePasswordAndPersist(user, newPassword);
  }

  public void changeLocale(UUID userId, Locale value) {
    RegisteredUser user = userDao.findById(userId).orElseThrow();
    if (Objects.equals(user.getPreferredLocale(), value)) return;
    log.info("User {} changes his preferred locale to {}", user.logEntity(), value);
    user.setPreferredLocale(value);
    userDao.createOrUpdate(user);
  }

  public void changePrimaryContactInfo(UUID userId, ContactInfo contactInfo) {
    RegisteredUser user = userDao.findById(userId).orElseThrow();
    if (Objects.equals(user.getPrimaryContactId(), contactInfo.id())) return;
    log.info("User {} changes his primary contact info to {}", user.logEntity(), contactInfo.id());
    user.setPrimaryContactId(contactInfo.id());
    userDao.createOrUpdate(user);
  }

  public void removeContact(UUID userId, ContactInfo contactInfo) {
    RegisteredUser user = userDao.findById(userId).orElseThrow();
    log.info("User {} removes contact info with id: {}", user.logEntity(), contactInfo.id());
    user.getContactInfos().remove(contactInfo);
    if (Objects.equals(user.getPrimaryContactId(), contactInfo.id())) {
      user.setPrimaryContactId(null);
    }
    userDao.createOrUpdate(user);
  }

  public RegisteredUser refreshUser(RegisteredUser currentUser) {
    return userDao.findOrThrow(currentUser.getId());
  }

  public void changeContactInfo(UUID userId, ContactInfo updatedContact) {
    if (updatedContact.id() == null
        || userDao.findOrThrow(userId).getContactInfos().stream()
            .noneMatch(contactInfo -> contactInfo.id().equals(updatedContact.id()))) {
      throw new IllegalArgumentException("Use addContactInfo on new contacts");
    }
    contactInfoModelDao.createOrUpdate(updatedContact.withUserId(userId));
  }
}

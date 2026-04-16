package org.bytewright.bgmo.usecases;

import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.Game;
import org.bytewright.bgmo.domain.model.data.HasUUID;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.automation.TimeSource;
import org.bytewright.bgmo.domain.service.data.GameDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserWorkflows {
  private final PasswordEncoder passwordEncoder;
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
                .passwordHash(passwordEncoder.encode(userDto.getPassword()))
                .build());
    Set<ContactInfo> contactInfos = newUser.getContactInfos();
    Optional.ofNullable(userDto.getSignalHandle())
        .map(
            s ->
                ContactInfo.SignalContact.builder().signalHandle(s).userId(newUser.getId()).build())
        .ifPresent(contactInfos::add);
    Optional.ofNullable(userDto.getTelegramHandle())
        .map(
            s ->
                ContactInfo.TelegramContact.builder()
                    .telegramHandle(s)
                    .userId(newUser.getId())
                    .build())
        .ifPresent(contactInfos::add);
    Optional.ofNullable(userDto.getEmail())
        .map(s -> ContactInfo.EmailContact.builder().email(s).userId(newUser.getId()).build())
        .ifPresent(contactInfos::add);
    if (contactInfos.isEmpty()) {
      throw new IllegalArgumentException("Users must provide at least one form of contact info!");
    }
    RegisteredUser refetchedUser = userDao.createOrUpdate(newUser);
    log.info("Created user with id {}: {}", refetchedUser.getId(), refetchedUser);
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
    ContactInfo contactInfoWithId = contactInfo.withUserId(userId);
    RegisteredUser user = userDao.findOrThrow(userId);
    user.getContactInfos().add(contactInfoWithId);
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
}

package org.bytewright.bgmo.usecases;

import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.Game;
import org.bytewright.bgmo.domain.model.data.HasUUID;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.GameDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserWorkflows {
  private final RegisteredUserDao userDao;
  private final GameDao gameDao;

  /**
   * User obj should have no id. For updating an existing model use {@link
   * RegisteredUserDao#createOrUpdate(HasUUID)}
   */
  public RegisteredUser create(RegisteredUser user) {
    if (user.getId() != null) {
      throw new IllegalArgumentException("User has an id already");
    }
    return userDao.createOrUpdate(user);
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
}

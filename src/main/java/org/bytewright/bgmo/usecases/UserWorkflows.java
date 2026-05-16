package org.bytewright.bgmo.usecases;

import static org.bytewright.bgmo.domain.model.user.UserStatus.AFTER_REGISTRATION;

import jakarta.transaction.Transactional;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.Game;
import org.bytewright.bgmo.domain.model.data.HasUUID;
import org.bytewright.bgmo.domain.model.user.*;
import org.bytewright.bgmo.domain.model.user.exception.ModifyContactsException;
import org.bytewright.bgmo.domain.service.InputSanitizer;
import org.bytewright.bgmo.domain.service.automation.TimeSource;
import org.bytewright.bgmo.domain.service.data.GameDao;
import org.bytewright.bgmo.domain.service.data.ModelDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.domain.service.event.EventPublisher;
import org.bytewright.bgmo.domain.service.security.BgmoUserDetailsService;
import org.bytewright.bgmo.domain.service.security.PasswordRules;
import org.bytewright.bgmo.domain.service.user.DisplayNameValidationService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserWorkflows {
  private final DisplayNameValidationService nameValidationService;
  private final BgmoUserDetailsService userDetailsService;
  private final ModelDao<ContactOption> contactOptionDao;
  private final InputSanitizer inputSanitizer;
  private final EventPublisher eventPublisher;
  private final RegisteredUserDao userDao;
  private final TimeSource timeSource;
  private final GameDao gameDao;

  /**
   * User obj should have no id. For updating an existing model use {@link
   * RegisteredUserDao#createOrUpdate(HasUUID)}
   */
  public RegisteredUser create(RegisteredUser.Creation userDto) {
    log.info("New user registered");
    RegisteredUser newUser =
        userDao.createOrUpdate(
            RegisteredUser.builder()
                .displayName(inputSanitizer.plainText(userDto.getDisplayName()))
                .loginName(userDto.getLoginName())
                .passwordHash(userDetailsService.hashPw(userDto.getPassword()))
                .preferredLocale(userDto.getPreferredLocale())
                .status(AFTER_REGISTRATION)
                .role(UserRole.USER)
                .build());
    RegisteredUser refetchedUser = userDao.createOrUpdate(newUser);
    log.info("Created user with id {}: {}", refetchedUser.getId(), refetchedUser);
    eventPublisher.publishUserCreatedAfterTransaction(refetchedUser.getId());
    return refetchedUser;
  }

  /**
   * Game obj should have no id. For updating an existing model use {@link
   * GameDao#createOrUpdate(HasUUID)}
   *
   * @see #addGameToLibrary(UUID, Game.Creation)
   */
  public Game updateGameInLibrary(UUID userId, Game game) {
    if (game.getId() == null) {
      throw new IllegalArgumentException("Must use addGameToLibrary for new games!");
    }
    log.info("Updating game of user {}: {}", userId, game.getName());
    return gameDao.createOrUpdate(game);
  }

  /**
   * Game obj should have no id. For updating an existing model use {@link
   * GameDao#createOrUpdate(HasUUID)}
   */
  public Game addGameToLibrary(UUID userId, Game.Creation gameDto) {
    List<String> cleanedTags = gameDto.getTags().stream().map(inputSanitizer::plainText).toList();
    Game newGame =
        Game.builder()
            .ownerId(userId)
            .name(inputSanitizer.plainText(gameDto.getName()))
            .description(inputSanitizer.plainText(gameDto.getDescription()))
            .notes(inputSanitizer.plainText(gameDto.getNotes()))
            .bggId(gameDto.getBggId())
            .minPlayers(gameDto.getMinPlayers())
            .maxPlayers(gameDto.getMaxPlayers())
            .optimalPlayers(gameDto.getOptimalPlayers())
            .artworkLink(gameDto.getArtworkLink())
            .playTimeMinutesPerPlayer(gameDto.getPlayTimeMinutesPerPlayer())
            .complexity(gameDto.getComplexity())
            .tags(cleanedTags)
            .urls(gameDto.getUrls())
            .build();
    Game persisted = gameDao.createOrUpdate(newGame);
    log.info("Adding game to user {}: {} (ID:{})", userId, persisted.getName(), persisted.id());
    return persisted;
  }

  /**
   * Adds a contact info entry to the user's profile.
   *
   * <p>If this is the user's first contact info, it is automatically set as primary.
   *
   * <p>If the user is in {@link UserStatus#AFTER_REGISTRATION} and had no prior contact info, their
   * status is promoted to {@link UserStatus#ACTIVE} within the same transaction.
   *
   * @return persisted user and new contactInfo
   */
  public UUID addContactInfo(UUID userId, ContactInfo contactInfo, boolean verified) {
    ContactOption newContact =
        ContactOption.builder()
            .contactInfo(contactInfo)
            .type(contactInfo.type())
            .userId(userId)
            .verified(verified)
            .build();

    ContactOption persistedContact = contactOptionDao.createOrUpdate(newContact);
    RegisteredUser user = userDao.findOrThrow(userId);

    // First contact automatically becomes the primary contact
    if (user.getPrimaryContactId() == null) {
      user.setPrimaryContactId(persistedContact.id());
    }

    if (user.getStatus() == AFTER_REGISTRATION) {
      user.setStatus(UserStatus.ACTIVE);
      log.info("User {} promoted to ACTIVE after adding contact info", user.logEntity());
    }
    userDao.createOrUpdate(user);
    return persistedContact.id();
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
    user.setDisplayName(inputSanitizer.plainText(newDisplayName));
    userDao.createOrUpdate(user);
  }

  public void changePassword(UUID userId, String newPassword) {
    RegisteredUser user = userDao.findById(userId).orElseThrow();
    if (newPassword.length() < PasswordRules.PW_MIN_CHARS
        || newPassword.length() > PasswordRules.PW_MAX_CHARS) {
      throw new IllegalArgumentException(
          "New PW for user %s violates password rules".formatted(userId));
    }
    userDetailsService.updatePasswordEncodeFirstAndPersist(user, newPassword);
  }

  public void changeLocale(UUID userId, Locale value) {
    RegisteredUser user = userDao.findById(userId).orElseThrow();
    if (Objects.equals(user.getPreferredLocale(), value)) return;
    log.info("User {} changes his preferred locale to {}", user.logEntity(), value);
    user.setPreferredLocale(value);
    userDao.createOrUpdate(user);
  }

  public void changePrimaryContactInfo(UUID userId, ContactOption contactOption) {
    RegisteredUser user = userDao.findOrThrow(userId);
    ContactOption refetchedOption = contactOptionDao.findOrThrow(contactOption.id());
    if (Objects.equals(user.getPrimaryContactId(), refetchedOption.id())) return;
    log.info(
        "User {} changes his primary contact info to {}", user.logEntity(), refetchedOption.id());
    user.setPrimaryContactId(refetchedOption.id());
    userDao.createOrUpdate(user);
  }

  public void removeContact(UUID userId, ContactOption contact) throws ModifyContactsException {
    RegisteredUser user = userDao.findById(userId).orElseThrow();
    if (user.getContactOptions().size() <= 1) {
      throw ModifyContactsException.lastContact();
    }
    log.info("User {} removes contact info with id: {}", user.logEntity(), contact.id());
    user.getContactOptions().remove(contact);
    if (Objects.equals(user.getPrimaryContactId(), contact.id())) {
      ContactOption newPrimary = user.getContactOptions().stream().findAny().orElseThrow();
      user.setPrimaryContactId(newPrimary.id());
    }
    userDao.createOrUpdate(user);
  }

  public boolean validateLoginName(String rawName) {
    return userDao.findByLoginName(rawName).isEmpty() && validateDisplayName(rawName);
  }

  public boolean validateDisplayName(String rawName) {
    ValidationResult validationResult = nameValidationService.validate(rawName);
    return switch (validationResult) {
      case ValidationResult.Failed failed -> {
        log.warn("Name validation service rejected a display name because: {}", failed.reason());
        yield false;
      }
      case ValidationResult.Success ignored -> true;
    };
  }
}

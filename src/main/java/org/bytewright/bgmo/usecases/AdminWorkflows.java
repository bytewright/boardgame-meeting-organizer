package org.bytewright.bgmo.usecases;

import static org.bytewright.bgmo.domain.service.AdapterSettingsProvider.ValidationResult.VALID;

import jakarta.transaction.Transactional;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.AdapterInfoAndSettings;
import org.bytewright.bgmo.domain.model.AdapterSettings;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.domain.model.RequestState;
import org.bytewright.bgmo.domain.model.notification.NotificationContext;
import org.bytewright.bgmo.domain.model.notification.NotificationPayload;
import org.bytewright.bgmo.domain.model.notification.NotificationTargetType;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.model.user.UserRole;
import org.bytewright.bgmo.domain.model.user.UserStatus;
import org.bytewright.bgmo.domain.service.AdapterSettingsProvider;
import org.bytewright.bgmo.domain.service.data.AdapterSettingsDao;
import org.bytewright.bgmo.domain.service.data.ModelDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.domain.service.event.EventPublisher;
import org.bytewright.bgmo.domain.service.notification.NotificationManager;
import org.bytewright.bgmo.domain.service.security.BgmoUserDetailsService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AdminWorkflows {
  private final Set<AdapterSettingsProvider> adapterSettingsProviders;
  private final BgmoUserDetailsService userDetailsService;
  private final NotificationManager notificationManager;
  private final AdapterSettingsDao adapterSettingsDao;
  private final ModelDao<MeetupJoinRequest> joinRequestModelDao;
  private final EventPublisher eventPublisher;
  private final RegisteredUserDao userDao;

  public void broadcastToAllUsers(UUID adminId, String msg) {
    RegisteredUser admin = userDao.findOrThrow(adminId);
    log.info("Admin '{}' triggers broadcast with message: {}", admin.logEntity(), msg);
    NotificationPayload payload =
        NotificationPayload.AdminBroadcast.builder()
            .message(msg)
            .adminName(admin.getDisplayName())
            .adminId(adminId)
            .build();
    for (RegisteredUser user :
        userDao.findAll().stream().filter(user -> user.getStatus() == UserStatus.ACTIVE).toList()) {
      Locale locale = Optional.ofNullable(user.getPreferredLocale()).orElse(Locale.GERMAN);
      notificationManager.dispatch(
          NotificationContext.builder()
              .userId(user.getId())
              .locale(locale)
              .notificationTargetType(NotificationTargetType.DIRECT)
              .payload(payload)
              .build());
    }
  }

  public void dispatchToUser(UUID adminId, UUID userId, String msg) {
    RegisteredUser admin = userDao.findOrThrow(adminId);
    RegisteredUser user = userDao.findOrThrow(userId);
    log.info(
        "Admin '{}' triggers msg dispatch to user {} with message: {}",
        admin.logEntity(),
        user.logEntity(),
        msg);
    NotificationPayload payload =
        NotificationPayload.AdminDirectMessage.builder()
            .message(msg)
            .adminName(admin.getDisplayName())
            .adminId(adminId)
            .build();
    Locale locale = Optional.ofNullable(user.getPreferredLocale()).orElse(Locale.GERMAN);
    notificationManager.dispatch(
        NotificationContext.builder()
            .userId(user.getId())
            .locale(locale)
            .notificationTargetType(NotificationTargetType.DIRECT)
            .payload(payload)
            .build());
  }

  public void approveUser(UUID adminId, UUID userToApprove) {
    RegisteredUser admin = userDao.findOrThrow(adminId);
    RegisteredUser user = userDao.findOrThrow(userToApprove);
    eventPublisher.publishUserVerifiedAfterTransaction(user.getId());
    RegisteredUser persisted = transitionStateToApproved(user);
    log.info(
        "Admin '{}' changes state of user {} to {}!",
        admin.logEntity(),
        user.logEntity(),
        persisted.getStatus());
  }

  private RegisteredUser transitionStateToApproved(RegisteredUser user) {
    user.setStatus(UserStatus.ACTIVE);
    return userDao.createOrUpdate(user);
  }

  public List<RegisteredUser> listNonActive() {
    RegisteredUser admin = findActiveAdmin();
    log.info("Admin '{}' lists all non active users", admin.logEntity());
    return userDao.findAll().stream()
        .filter(registeredUser -> registeredUser.getStatus().isLocked())
        .sorted(Comparator.comparing(RegisteredUser::getTsCreation))
        .toList();
  }

  private RegisteredUser findActiveAdmin() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Objects.requireNonNull(authentication);
    User user = (User) authentication.getPrincipal();
    Objects.requireNonNull(user);
    UUID userId = UUID.fromString(user.getUsername());
    RegisteredUser appUser = userDao.findOrThrow(userId);
    if (appUser.getRole() != UserRole.ADMIN) {
      throw new IllegalStateException("user is not admin: " + userId);
    }
    return appUser;
  }

  public RegisteredUser makeAdmin(UUID userId) {
    RegisteredUser user = userDao.findOrThrow(userId);
    log.info("User '{}' is promoted to role admin!", user.logEntity());
    user.setRole(UserRole.ADMIN);
    return userDao.createOrUpdate(user);
  }

  public List<RegisteredUser> listAllUsers() {
    RegisteredUser admin = findActiveAdmin();
    log.info("Admin '{}' requested full user list", admin.logEntity());
    return userDao.findAll().stream()
        .sorted(Comparator.comparing(RegisteredUser::getTsCreation).reversed())
        .toList();
  }

  public String getRegistrationIntroText(UUID userId) {
    return userDao.getRegistrationIntroText(userId).orElse("Kein Text hinterlegt.");
  }

  public void toggleUserBanStatus(UUID userId) {
    RegisteredUser admin = findActiveAdmin();
    RegisteredUser user = userDao.findOrThrow(userId);

    if (user.getStatus() == UserStatus.BANNED) {
      log.info("Admin '{}' unbanned user {}", admin.logEntity(), user.logEntity());
      user.setStatus(UserStatus.ACTIVE);
    } else {
      log.info("Admin '{}' banned user {}", admin.logEntity(), user.logEntity());
      user.setStatus(UserStatus.BANNED);
    }
    userDao.createOrUpdate(user);
  }

  /**
   * Sets a user's status directly. Intended for admin UI dropdowns / status management. Prefer
   * specific workflow methods (approveUser, toggleUserBanStatus) for business-logic-heavy
   * transitions; use this for administrative overrides.
   */
  public void setUserStatus(UUID userId, UserStatus newStatus) {
    RegisteredUser admin = findActiveAdmin();
    RegisteredUser user = userDao.findOrThrow(userId);
    log.info(
        "Admin '{}' sets status of user {} from {} to {}",
        admin.logEntity(),
        user.logEntity(),
        user.getStatus(),
        newStatus);
    user.setStatus(newStatus);
    userDao.createOrUpdate(user);
  }

  /** Promotes or demotes a user's role. */
  public void setUserRole(UUID userId, UserRole newRole) {
    RegisteredUser admin = findActiveAdmin();
    RegisteredUser user = userDao.findOrThrow(userId);
    log.info(
        "Admin '{}' sets role of user {} from {} to {}",
        admin.logEntity(),
        user.logEntity(),
        user.getRole(),
        newRole);
    user.setRole(newRole);
    userDao.createOrUpdate(user);
  }

  public String generateAndSetTemporaryPassword(UUID userId) {
    RegisteredUser admin = findActiveAdmin();
    RegisteredUser user = userDao.findOrThrow(userId);

    // Generate a simple 8-character alphanumeric password
    String tempPassword = UUID.randomUUID().toString().substring(0, 8);

    log.info(
        "Admin '{}' generated a temporary password for user {}",
        admin.logEntity(),
        user.logEntity());
    userDetailsService.updatePasswordEncodeFirstAndPersist(user, tempPassword);
    return tempPassword;
  }

  public void updateAdapterSettings(AdapterSettings settings, String newJson) {
    AdapterSettingsProvider provider =
        adapterSettingsProviders.stream()
            .filter(asp -> asp.getAdapterInfo().stableName().equals(settings.getAdapterName()))
            .findAny()
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "There is no adapter in app context with name: "
                            + settings.getAdapterName()));
    AdapterSettings adapterSettings = adapterSettingsDao.findByAdapter(provider.getAdapterInfo());
    try {
      var validationResult = provider.isValidSettingsJson(adapterSettings.getAdapterSettings());
      if (validationResult == VALID) {
        adapterSettingsDao.createOrUpdate(settings.toBuilder().adapterSettings(newJson).build());
        return;
      }
    } catch (Exception e) {
      log.error(
          "Provider crashed during validation: {}", provider.getAdapterInfo().stableName(), e);
    }
    throw new IllegalArgumentException(
        "Adapter '%s' rejected new settings as invalid. Check json syntax and content!"
            .formatted(provider.getAdapterInfo()));
  }

  /** sorted by name */
  public List<AdapterInfoAndSettings> findAllAdaptersAndSettings() {
    List<AdapterInfoAndSettings> infoAndSettings = new ArrayList<>();
    for (AdapterSettingsProvider provider :
        adapterSettingsProviders.stream()
            .sorted(Comparator.comparing(p -> p.getAdapterInfo().stableName()))
            .toList()) {
      AdapterSettingsProvider.AdapterInfo adapterInfo = provider.getAdapterInfo();
      AdapterSettings adapterSettings = adapterSettingsDao.findByAdapter(adapterInfo);
      infoAndSettings.add(new AdapterInfoAndSettings(adapterInfo, adapterSettings));
    }
    return infoAndSettings;
  }

  public AdapterSettingsProvider.ValidationResult validateJson(
      AdapterSettingsProvider.AdapterInfo adapterInfo, String json) {
    Optional<AdapterSettingsProvider> provider =
        adapterSettingsProviders.stream()
            .filter(asp -> asp.getAdapterInfo().stableName().equals(adapterInfo.stableName()))
            .findAny();
    if (provider.isEmpty()) {
      log.warn(
          "Can't validate json because provider could not be found! {}", adapterInfo.stableName());
      return AdapterSettingsProvider.ValidationResult.INVALID;
    }
    return provider
        .map(p -> p.isValidSettingsJson(json))
        .orElse(AdapterSettingsProvider.ValidationResult.INVALID);
  }

  /** Hard-deletes a join request from the database. */
  public void deleteJoinRequest(UUID joinRequestId) {
    MeetupJoinRequest request = joinRequestModelDao.findOrThrow(joinRequestId);
    log.warn(
        "ADMIN: Hard-deleting join request {} for meetup {}", joinRequestId, request.getMeetupId());
    joinRequestModelDao.delete(joinRequestId);
    // todo publish so waitlist gets updated
  }

  /** Revokes an ACCEPTED join request, transitioning it back to OPEN. */
  public void revokeAttendeeConfirmation(UUID joinRequestId) {
    MeetupJoinRequest request = joinRequestModelDao.findOrThrow(joinRequestId);
    if (request.getRequestState() != RequestState.ACCEPTED) {
      throw new IllegalStateException(
          "Can only revoke ACCEPTED requests, current state: " + request.getRequestState());
    }
    log.warn(
        "ADMIN: Revoking confirmation for join request {} on meetup {}",
        joinRequestId,
        request.getMeetupId());
    request.setRequestState(RequestState.OPEN);
    joinRequestModelDao.createOrUpdate(request);
    // todo publish so waitlist gets updated
  }
}

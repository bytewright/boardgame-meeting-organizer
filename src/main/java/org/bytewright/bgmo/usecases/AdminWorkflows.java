package org.bytewright.bgmo.usecases;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.model.user.UserStatus;
import org.bytewright.bgmo.domain.service.automation.NotificationManager;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminWorkflows {
  private final NotificationManager notificationManager;
  private final RegisteredUserDao userDao;

  public RegisteredUser approveUser(UUID adminId, UUID userToApprove) {
    RegisteredUser admin = userDao.findOrThrow(adminId);
    RegisteredUser user = userDao.findOrThrow(userToApprove);
    log.info(
        "Admin '{}' changes state of user {} to approved!", admin.logEntity(), user.logEntity());
    notificationManager.addUserApprovedTask(user.getId());
    return transitionStateToApproved(user);
  }

  private RegisteredUser transitionStateToApproved(RegisteredUser user) {
    user.setStatus(UserStatus.ACTIVE);
    return userDao.createOrUpdate(user);
  }

  public List<RegisteredUser> listNonActive(UUID adminId) {
    RegisteredUser admin = userDao.findOrThrow(adminId);
    log.info("Admin '{}' lists all non active users", admin.logEntity());
    return userDao.findAll().stream()
        .filter(registeredUser -> registeredUser.getStatus().isLocked())
        .sorted(Comparator.comparing(RegisteredUser::getTsCreation))
        .toList();
  }
}

package org.bytewright.bgmo.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;
import org.bytewright.bgmo.domain.model.Game;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactOption;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.model.user.UserStatus;
import org.bytewright.bgmo.domain.service.data.ModelDao;
import org.bytewright.bgmo.testutils.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserWorkflowsTest extends IntegrationTest {
  @Autowired private UserWorkflows userWorkflows;
  @Autowired private ModelDao<ContactOption> contactOptionModelDao;

  @Test
  void testAddGame() {
    // ARRANGE
    RegisteredUser user = helper.user();
    Game.Creation game = Game.Creation.builder().name("Testgame").build();
    // ACT
    Game persisted = userWorkflows.addGameToLibrary(user.getId(), game);
    // ASSERT
    assertThat(persisted.getId()).isNotNull();
    assertThat(persisted.getName()).isEqualTo("Testgame");
    assertThat(persisted.getOwnerId()).isEqualTo(user.getId());
  }

  @Test
  void testAddContactInfos() {
    // ARRANGE
    RegisteredUser user = helper.user();
    assertThat(user).returns(UserStatus.AFTER_REGISTRATION, RegisteredUser::getStatus);
    ContactInfo.EmailContact emailContact =
        ContactInfo.EmailContact.builder().email("someEmail@mail.com").build();
    // ACT
    UUID uuid = userWorkflows.addContactInfo(user.getId(), emailContact, false);
    // ASSERT
    user = userDao.findOrThrow(user.getId());
    Set<ContactOption> contactInfos = user.getContactOptions();
    assertThat(contactInfos).hasSize(1);
    assertThat(user).returns(UserStatus.ACTIVE, RegisteredUser::getStatus);
    ContactOption contactInfo = contactOptionModelDao.findOrThrow(uuid);

    assertThat(user).returns(contactInfo.id(), RegisteredUser::getPrimaryContactId);
    assertThat(contactInfo.getContactInfo()).isInstanceOf(ContactInfo.EmailContact.class);
    ContactInfo.EmailContact emailContactPersisted =
        (ContactInfo.EmailContact) contactInfo.getContactInfo();
    assertThat(emailContactPersisted)
        .returns("someEmail@mail.com", ContactInfo.EmailContact::email)
        .isNotNull();
  }
}

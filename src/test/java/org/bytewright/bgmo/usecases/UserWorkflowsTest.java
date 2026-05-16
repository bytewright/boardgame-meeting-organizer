package org.bytewright.bgmo.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.bytewright.bgmo.domain.model.Game;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.model.user.UserStatus;
import org.bytewright.bgmo.testutils.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserWorkflowsTest extends IntegrationTest {
  @Autowired private UserWorkflows userWorkflows;

  @Test
  void testAddGame() {
    // ARRANGE
    RegisteredUser user = helper.user();
    Game game = Game.builder().name("Testgame").build();
    // ACT
    Game persisted = userWorkflows.updateGameInLibrary(user.getId(), game);
    // ASSERT
    assertThat(persisted.getId()).isNotNull();
  }

  @Test
  void testAddContactInfos() {
    // ARRANGE
    RegisteredUser user = helper.user();
    assertThat(user).returns(UserStatus.AFTER_REGISTRATION, RegisteredUser::getStatus);
    ContactInfo.EmailContact emailContact =
        ContactInfo.EmailContact.builder().email("someEmail@mail.com").build();
    // ACT
    var persisted = userWorkflows.addContactInfo(user.getId(), emailContact, false);
    // ASSERT
    Set<ContactInfo> contactInfos = persisted.getKey().getContactOptions();
    assertThat(contactInfos).hasSize(1);
    assertThat(persisted.getKey()).returns(UserStatus.ACTIVE, RegisteredUser::getStatus);
    ContactInfo contactInfo = persisted.getValue();

    assertThat(user).returns(contactInfo.id(), RegisteredUser::getPrimaryContactId);
    assertThat(contactInfo).isInstanceOf(ContactInfo.EmailContact.class);
    ContactInfo.EmailContact emailContactPersisted = (ContactInfo.EmailContact) contactInfo;
    assertThat(emailContactPersisted)
        .returns(user.getId(), ContactInfo.EmailContact::userId)
        .returns("someEmail@mail.com", ContactInfo.EmailContact::email)
        .extracting(ContactInfo.EmailContact::id)
        .isNotNull();
  }
}

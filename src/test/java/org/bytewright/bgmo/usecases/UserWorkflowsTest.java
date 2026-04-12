package org.bytewright.bgmo.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.bytewright.bgmo.domain.model.Game;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
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
    Game persisted = userWorkflows.addGameToLibrary(user.getId(), game);
    // ASSERT
    assertThat(persisted.getId()).isNotNull();
  }
}

package org.bytewright.bgmo.adapter.bgg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import lombok.SneakyThrows;
import org.bytewright.bgmo.domain.model.Game;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BggXmlParserTest {
  @InjectMocks private BggXmlParser testee;

  @SneakyThrows
  @Test
  void testLoadXml() {
    // ARRANGE
    long bggId = 224517;
    String xml = Files.readString(Path.of("src/test/resources/bgg/224517-17ff25.xml"));
    // ACT
    Optional<Game.Creation> creation = testee.parseGameCreation(xml, bggId);
    // ASSERT
    assertThat(creation).isPresent();
    assertThat(creation.get())
        .returns(224517L, Game.Creation::getBggId)
        .returns(2, Game.Creation::getMinPlayers)
        .returns(4, Game.Creation::getMaxPlayers)
        .returns(45, Game.Creation::getPlayTimeMinutesPerPlayer)
        .returns(20, c -> c.getTags().size());
  }
}

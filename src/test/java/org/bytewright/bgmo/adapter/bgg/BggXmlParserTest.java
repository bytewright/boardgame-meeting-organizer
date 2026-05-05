package org.bytewright.bgmo.adapter.bgg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
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
    Path zip = Path.of("src/test/resources/bgg/bgg-fixtures.zip");
    String xml;
    try (ZipFile zf = new ZipFile(zip.toFile())) {
      ZipEntry entry = zf.getEntry("224517-17ff25.xml");
      try (InputStream is = zf.getInputStream(entry)) {
        xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      }
    }
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

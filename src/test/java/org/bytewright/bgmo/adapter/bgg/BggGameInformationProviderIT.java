package org.bytewright.bgmo.adapter.bgg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.bytewright.bgmo.domain.model.Game;
import org.bytewright.bgmo.domain.service.GameInformationProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.RestClient;

/**
 * Integration test that verifies the full BGG adapter chain against the live BGG API. <br>
 * Requires network access.
 */
// @Tag("integration")
@SpringJUnitConfig(classes = {BggGameInformationProviderIT.TestConfig.class})
class BggGameInformationProviderIT {

  // Well-known BGG IDs used as test fixtures — stable, popular games unlikely to be removed
  private static final long BGG_ID_GLOOMHAVEN = 174430L; // Gloomhaven
  private static final long BGG_ID_PANDEMIC = 30549L; // Pandemic
  private static final long BGG_ID_TICKET_TO_RIDE = 9209L; // Ticket to Ride

  @Autowired private GameInformationProvider provider;

  // -------------------------------------------------------------------------
  // Happy path
  // -------------------------------------------------------------------------

  @Test
  void fetchGloomhaven_returnsCorrectCoreFields() {
    Optional<Game.Creation> result = provider.generateGame(String.valueOf(BGG_ID_GLOOMHAVEN));

    assertThat(result).isPresent();
    Game.Creation game = result.get();

    assertThat(game.getName()).isEqualTo("Gloomhaven");
    assertThat(game.getBggId()).isEqualTo(BGG_ID_GLOOMHAVEN);
    assertThat(game.getMinPlayers()).isEqualTo(1);
    assertThat(game.getMaxPlayers()).isEqualTo(4);
  }

  @Test
  void fetchGloomhaven_returnsComplexityInValidRange() {
    Game.Creation game = fetchOrFail(BGG_ID_GLOOMHAVEN);

    // Gloomhaven is a heavy game; BGG averageweight consistently > 3.5
    assertThat(game.getComplexity()).isNotNull().isBetween(1.0, 5.0);
    assertThat(game.getComplexity()).isGreaterThan(3.0);
  }

  @Test
  void fetchGloomhaven_returnsOptimalPlayerCount() {
    Game.Creation game = fetchOrFail(BGG_ID_GLOOMHAVEN);

    // Community consensus for Gloomhaven is 2–3 players best; value must be in [min, max]
    assertThat(game.getOptimalPlayers())
        .isNotNull()
        .isBetween(game.getMinPlayers(), game.getMaxPlayers());
  }

  @Test
  void fetchGloomhaven_returnsArtworkAndBggUrl() {
    Game.Creation game = fetchOrFail(BGG_ID_GLOOMHAVEN);

    assertThat(game.getArtworkLink()).isNotNull().isNotBlank().startsWith("https://");

    assertThat(game.getUrls())
        .isNotEmpty()
        .contains("https://boardgamegeek.com/boardgame/" + BGG_ID_GLOOMHAVEN);
  }

  @Test
  void fetchPandemic_returnsCorrectPlayerRange() {
    Game.Creation game = fetchOrFail(BGG_ID_PANDEMIC);

    assertThat(game.getName()).isEqualTo("Pandemic");
    assertThat(game.getBggId()).isEqualTo(BGG_ID_PANDEMIC);
    assertThat(game.getMinPlayers()).isEqualTo(2);
    assertThat(game.getMaxPlayers()).isEqualTo(4);
  }

  @Test
  void fetchTicketToRide_returnsDescription() {
    Game.Creation game = fetchOrFail(BGG_ID_TICKET_TO_RIDE);

    assertThat(game.getDescription())
        .isNotNull()
        .isNotBlank()
        .hasSizeGreaterThan(20); // not just a stub
  }

  // -------------------------------------------------------------------------
  // Input validation (no network call expected)
  // -------------------------------------------------------------------------

  @Test
  void nonNumericInput_returnsEmpty() {
    Optional<Game.Creation> result = provider.generateGame("Gloomhaven");
    assertThat(result).isEmpty();
  }

  @Test
  void blankInput_returnsEmpty() {
    Optional<Game.Creation> result = provider.generateGame("   ");
    assertThat(result).isEmpty();
  }

  @Test
  void emptyString_returnsEmpty() {
    Optional<Game.Creation> result = provider.generateGame("");
    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // Error handling (network call, unknown BGG ID)
  // -------------------------------------------------------------------------

  @Test
  void unknownBggId_returnsEmpty() {
    // BGG returns an empty <items> element for IDs that do not exist
    Optional<Game.Creation> result = provider.generateGame("999999999");
    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // Minimal Spring context — only the BGG adapter beans, no full app boot
  // -------------------------------------------------------------------------

  static class TestConfig {

    @Bean
    RestClient bggRestClient() {
      return RestClient.builder()
          .baseUrl("https://boardgamegeek.com/xmlapi2")
          .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE)
          .build();
    }

    @Bean
    BggXmlParser bggXmlParser() {
      return new BggXmlParser();
    }

    @Bean
    BggApiClient bggApiClient(RestClient bggRestClient) {
      return new BggApiClient(bggRestClient);
    }

    @Bean
    GameInformationProvider gameInformationProvider(
        BggApiClient bggApiClient, BggXmlParser bggXmlParser) {
      return new BggGameInformationProvider(bggApiClient, bggXmlParser);
    }
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private Game.Creation fetchOrFail(long bggId) {
    return provider
        .generateGame(String.valueOf(bggId))
        .orElseThrow(
            () -> new AssertionError("Expected a result for bggId=" + bggId + " but got empty"));
  }
}

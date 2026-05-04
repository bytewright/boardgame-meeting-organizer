package org.bytewright.bgmo.adapter.bgg;

import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.AdapterSettings;
import org.bytewright.bgmo.domain.model.Game;
import org.bytewright.bgmo.domain.service.AdapterSettingsProvider;
import org.bytewright.bgmo.domain.service.GameInformationProvider;
import org.bytewright.bgmo.domain.service.JsonMapperFactory;
import org.bytewright.bgmo.domain.service.data.AdapterSettingsDao;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * BGG-backed implementation of the {@link GameInformationProvider} port.
 *
 * <p>Accepts a numeric BGG ID as user input (e.g. "174430" for Gloomhaven). Non-numeric input is
 * rejected immediately with an empty result rather than hitting the remote API unnecessarily.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BggGameInformationProvider
    implements GameInformationProvider, AdapterSettingsProvider {
  static final String ADAPTER_NAME = "BggGameInformationProvider-integration";
  private final BggAdapterProperties bggAdapterProperties;
  private final AdapterSettingsDao adapterSettingsDao;
  private final MessageSource messageSource;
  private final JsonMapper objectMapper = JsonMapperFactory.unRedactedMapper();
  private final BggApiClient bggApiClient;
  private final BggXmlParser xmlParser;

  @Override
  public InputConfig getInputConfig(Locale locale) {
    BggAdapterSettings settings = getSettings();

    String message = messageSource.getMessage("bgg.input.hint", null, locale);
    return InputConfig.builder()
        .providerDisplayName(settings.getDisplayName())
        .providerLogoLink(settings.getLogoLink())
        .inputHint(message)
        .inputDataValidator(s -> parseUserInput(s).isPresent())
        .build();
  }

  private boolean isEnabled() {
    BggAdapterSettings settings = getSettings();
    return bggAdapterProperties.isEnabled() && settings.isEnabled();
  }

  private Optional<Long> parseUserInput(String input) {
    try {
      return Optional.of(Long.parseLong(input.strip()));
    } catch (NumberFormatException e) {
      log.debug("BGG adapter requires a numeric BGG ID; received: '{}'", input);
      return Optional.empty();
    }
  }

  @SneakyThrows
  private BggAdapterSettings getSettings() {
    AdapterSettings adapterSettings = adapterSettingsDao.findByAdapterName(ADAPTER_NAME);
    return objectMapper.readValue(adapterSettings.getAdapterSettings(), BggAdapterSettings.class);
  }

  /**
   * {@inheritDoc}
   *
   * @param userInput the numeric BGG ID provided by the user
   * @return a populated {@link Game.Creation}, or empty if the ID is non-numeric, the game does not
   *     exist on BGG, or the API is unreachable
   */
  @Override
  public Optional<Game.Creation> generateGame(String userInput) {
    Optional<Long> optionalLong = parseUserInput(userInput);
    if (optionalLong.isEmpty()) return Optional.empty();
    long bggId = optionalLong.get();
    if (!isEnabled()) {
      return mockBggCreation(bggId);
    }
    log.info("Fetching game information from BGG for id={}", bggId);
    Optional<String> xmlOpt = bggApiClient.fetchGame(bggId);
    if (xmlOpt.isEmpty()) {
      log.warn("Failed to use bgg api to fetch information about {}", bggId);
      return Optional.empty();
    }
    Optional<Game.Creation> creation = xmlParser.parseGameCreation(xmlOpt.get(), bggId);
    log.info(
        "Finished fetching boardGame info from bgg for id {}: success? {}",
        bggId,
        creation.isPresent());
    return creation;
  }

  private Optional<Game.Creation> mockBggCreation(long bggId) {
    return Optional.of(
        Game.Creation.builder()
            .bggId(bggId)
            .url(new Game.UserLink("https://boardgamegeek.com/boardgame/" + bggId, "BGG link"))
            .build());
  }

  @Override
  public String getAdapterName() {
    return ADAPTER_NAME;
  }

  @Override
  public boolean isValidSettingsJson(String jsonData) {
    try {
      var settings = objectMapper.readValue(jsonData, BggAdapterSettings.class);
      return settings != null;
    } catch (JacksonException e) {
      log.error("Provided settings are invalid", e);
    }
    return false;
  }

  @Override
  public String getDefaultSettings() throws Exception {
    return objectMapper.writeValueAsString(BggAdapterSettings.builder().build());
  }
}

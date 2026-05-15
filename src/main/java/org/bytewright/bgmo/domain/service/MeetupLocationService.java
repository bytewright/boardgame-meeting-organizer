package org.bytewright.bgmo.domain.service;

import java.util.*;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.AdapterSettings;
import org.bytewright.bgmo.domain.model.MeetupEventLocation;
import org.bytewright.bgmo.domain.model.MeetupLocationSuggestion;
import org.bytewright.bgmo.domain.service.data.AdapterSettingsDao;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetupLocationService implements AdapterSettingsProvider {
  private static final String ADAPTER_NAME = "core.meetup-locations-service";
  private final JsonMapper mapper = JsonMapperFactory.unRedactedMapper();
  private final AdapterSettingsDao adapterSettingsDao;
  private final MeetupDao meetupDao;

  /**
   * Returns a merged, deduplicated list of:
   *
   * <ul>
   *   <li>Locations previously entered by the given user across their past meetups (personal).
   *   <li>Site-wide common locations maintained by admins (common).
   * </ul>
   *
   * Common locations first.
   *
   * @param userId the organizer's UUID
   * @return ordered list of suggestions
   */
  public List<MeetupLocationSuggestion> getSuggestedLocations(UUID userId) {
    Set<MeetupEventLocation> allLocationsByOrganizer =
        meetupDao.findAllLocationsByOrganizer(userId);
    List<MeetupLocationSuggestion> allLocationsForUser =
        Stream.concat(
                getSettings().locations().stream()
                    .filter(PersistedLocation::enabled)
                    .map(l -> new MeetupEventLocation(l.areaHint(), l.fullLocation()))
                    .map(MeetupLocationSuggestion::common),
                allLocationsByOrganizer.stream().map(MeetupLocationSuggestion::personal))
            .toList();
    List<MeetupLocationSuggestion> deduplicatedLocations = new ArrayList<>(allLocationsForUser);
    Set<String> seenLocations = new HashSet<>();
    for (MeetupLocationSuggestion location : allLocationsForUser) {
      String key = location.location().areaHint() + location.location().fullLocation();
      if (!seenLocations.add(key)) deduplicatedLocations.remove(location);
    }
    return deduplicatedLocations;
  }

  @Override
  public AdapterSettingsProvider.AdapterInfo getAdapterInfo() {
    return AdapterSettingsProvider.AdapterInfo.builder()
        .stableName(ADAPTER_NAME)
        .description(
            "Provides common meetup locations and fetches previous user-created meetup locations")
        .build();
  }

  private LocationServiceSettings getSettings() {
    AdapterSettings settings = adapterSettingsDao.findByAdapter(getAdapterInfo());
    String adapterSettings = settings.getAdapterSettings();
    return mapper.readValue(adapterSettings, LocationServiceSettings.class);
  }

  @Override
  public ValidationResult isValidSettingsJson(String jsonData) {
    try {
      var settings = mapper.readValue(jsonData, LocationServiceSettings.class);
      return settings != null ? ValidationResult.VALID : ValidationResult.INVALID;
    } catch (JacksonException e) {
      log.error("Error while validating json: {}", e.getMessage());
    }
    return ValidationResult.INVALID;
  }

  @Override
  public String getDefaultSettings() throws Exception {
    return mapper.writeValueAsString(
        LocationServiceSettings.builder()
            .location(
                PersistedLocation.builder()
                    .areaHint("Central Park")
                    .fullLocation("The fountain near whatever street")
                    .enabled(false)
                    .build())
            .build());
  }

  @Builder
  private record LocationServiceSettings(@Singular List<PersistedLocation> locations) {}

  @Builder
  public record PersistedLocation(String areaHint, String fullLocation, boolean enabled) {}
}

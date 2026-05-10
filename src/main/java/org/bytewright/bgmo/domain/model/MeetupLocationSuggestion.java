package org.bytewright.bgmo.domain.model;

/**
 * A location suggestion presented to the organizer during meetup creation.
 *
 * <ul>
 *   <li><b>Personal</b> locations the user has previously entered for their own meetups, derived
 *       from past {@link MeetupEvent} records.
 *   <li><b>Common</b> curated locations maintained by a site admin (e.g. a local board-game café
 *       that is frequently used). Managed via the admin panel.
 * </ul>
 *
 * @param location the actual location data
 * @param type {@code PUBLIC} if this is a site-wide common location; {@code PERSONAL} if personal
 */
public record MeetupLocationSuggestion(MeetupEventLocation location, SuggestionType type) {
  public boolean isCommon() {
    return type() == SuggestionType.PUBLIC;
  }

  public enum SuggestionType {
    PUBLIC,
    PERSONAL
  }

  public static MeetupLocationSuggestion common(MeetupEventLocation location) {
    return new MeetupLocationSuggestion(location, SuggestionType.PUBLIC);
  }

  public static MeetupLocationSuggestion personal(MeetupEventLocation location) {
    return new MeetupLocationSuggestion(location, SuggestionType.PERSONAL);
  }
}

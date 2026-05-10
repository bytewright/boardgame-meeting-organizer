package org.bytewright.bgmo.domain.model;

/**
 * Describes where a meetup takes place.
 *
 * <p>The two fields serve different audiences:
 *
 * <ul>
 *   <li>{@link #areaHint()} — always public. Shown to everyone (including non-attendees browsing
 *       the dashboard) so they know the rough area. Can be a postcode, a neighbourhood name, or a
 *       landmark reference like "Park near the old bridge".
 *   <li>{@link #fullLocation()} — restricted. Shown only to confirmed attendees and the organizer.
 *       Contains the precise address, building name, buzzer code, or any other detail needed to
 *       actually find the place.
 * </ul>
 *
 * <p>Both fields are free-form strings — no structured address parsing is performed.
 *
 * @param areaHint public rough-area descriptor, always visible
 * @param fullLocation precise location details, visible to attendees only
 */
public record MeetupEventLocation(String areaHint, String fullLocation) {}

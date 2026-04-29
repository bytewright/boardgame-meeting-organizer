package org.bytewright.bgmo.adapter.api.frontend.service;

import static org.bytewright.bgmo.domain.model.RequestState.*;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import org.bytewright.bgmo.adapter.api.frontend.view.meetup.ViewerRole;
import org.bytewright.bgmo.domain.model.Game;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;

/**
 * Immutable view-model computed once per page load and passed to all components.
 *
 * <p>Centralises all role detection and data pre-fetching so individual components never have to
 * query state independently.
 *
 * @param meetup the event being displayed
 * @param currentUser null when the visitor is not logged in
 * @param role the computed relationship of this visitor to the event
 * @param myRequest the visitor's own join request, if any
 * @param isFull whether all slots are taken (false when slots are unlimited)
 * @param offeredGames fully hydrated game objects (empty list when none offered)
 * @param creatorDisplayName display name of the organiser, fallback to UUID string
 * @param zipCode organiser's zip / postal code — shown to everyone when present
 * @param creatorAddress full organiser address — only exposed to components when {@link
 *     #showFullAddress()} returns true
 */
public record MeetupDetailContext(
    MeetupEvent meetup,
    @Nullable RegisteredUser currentUser,
    ViewerRole role,
    Optional<MeetupJoinRequest> myRequest,
    boolean isFull,
    List<Game> offeredGames,
    String creatorDisplayName,
    Optional<String> zipCode,
    Optional<ContactInfo.AddressContact> creatorAddress) {

  /** Everyone sees the zip code when the organiser has an address on file. */
  public boolean showZipCode() {
    return zipCode.isPresent();
  }

  /**
   * Only accepted attendees (anon or registered) and the organiser see the full address.
   *
   * <p>This is the single authoritative check — no other component should re-derive it.
   */
  public boolean showFullAddress() {
    return role == ViewerRole.ANON_ACCEPTED
        || role == ViewerRole.REGISTERED_ACCEPTED
        || role == ViewerRole.ORGANIZER;
  }

  /** Convenience: is the viewer in any kind of organiser role? */
  public boolean isOrganizer() {
    return role == ViewerRole.ORGANIZER;
  }

  /** Convenience: pending open-request count (useful for organiser badge). */
  public long pendingRequestCount() {
    return meetup.getJoinRequests().stream().filter(r -> r.getRequestState() == OPEN).count();
  }
}

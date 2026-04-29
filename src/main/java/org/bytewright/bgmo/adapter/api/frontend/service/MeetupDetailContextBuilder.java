package org.bytewright.bgmo.adapter.api.frontend.service;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.api.frontend.view.meetup.ViewerRole;
import org.bytewright.bgmo.domain.model.Game;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.domain.model.RequestState;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.GameDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.usecases.MeetupWorkflows;
import org.springframework.stereotype.Component;

/**
 * Builds a {@link MeetupDetailContext} from raw domain objects.
 *
 * <p>This is the single place where viewer role is determined and where all necessary data (games,
 * creator address) is pre-fetched for the page load. Components downstream must not perform their
 * own DAO calls.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetupDetailContextBuilder {

  private final MeetupWorkflows meetupWorkflows;
  private final GameDao gameDao;
  private final RegisteredUserDao userDao;

  /**
   * Builds the context for the given meetup and visitor.
   *
   * @param meetup the event (freshly fetched)
   * @param currentUser null if the visitor is not authenticated
   * @param anonToken the session token for anonymous visitors, null if none stored yet
   */
  public MeetupDetailContext build(
      MeetupEvent meetup, @Nullable RegisteredUser currentUser, @Nullable UUID anonToken) {

    List<Game> offeredGames =
        meetup.getOfferedGames() == null || meetup.getOfferedGames().isEmpty()
            ? List.of()
            : gameDao.findAllById(meetup.getOfferedGames());

    boolean isFull = meetupWorkflows.isFull(meetup);

    String creatorDisplayName =
        userDao
            .findById(meetup.getCreatorId())
            .map(RegisteredUser::getDisplayName)
            .orElseGet(() -> meetup.getCreatorId().toString());

    // Organiser address — fetched once for the whole page.
    Optional<ContactInfo.AddressContact> creatorAddress =
        userDao.findById(meetup.getCreatorId()).stream()
            .flatMap(u -> u.getContactInfos().stream())
            .filter(ContactInfo.AddressContact.class::isInstance)
            .map(ContactInfo.AddressContact.class::cast)
            .findFirst();

    Optional<String> zipCode = creatorAddress.map(ContactInfo.AddressContact::zipCode);

    // ── Determine viewer role ────────────────────────────────────────────────
    ViewerRole role;
    Optional<MeetupJoinRequest> myRequest = Optional.empty();

    if (currentUser != null && meetup.getCreatorId().equals(currentUser.getId())) {
      role = ViewerRole.ORGANIZER;
    } else if (currentUser != null) {
      myRequest =
          meetup.getJoinRequests().stream()
              .filter(r -> currentUser.getId().equals(r.getUserId()))
              .findFirst();

      role =
          myRequest
              .map(
                  r ->
                      switch (r.getRequestState()) {
                        case OPEN, DECLINED -> ViewerRole.REGISTERED_PENDING;
                        case ACCEPTED -> ViewerRole.REGISTERED_ACCEPTED;
                        case CANCELED -> ViewerRole.REGISTERED_CANCELED;
                      })
              .orElse(ViewerRole.REGISTERED_PENDING);
    } else if (anonToken != null) {
      myRequest =
          meetup.getJoinRequests().stream()
              .filter(r -> anonToken.equals(r.getAnonToken()))
              .findFirst();

      role =
          myRequest
              .map(
                  r ->
                      r.getRequestState() == RequestState.ACCEPTED
                          ? ViewerRole.ANON_ACCEPTED
                          : ViewerRole.ANON_PENDING)
              .orElse(ViewerRole.ANONYMOUS);
    } else {
      role = ViewerRole.ANONYMOUS;
    }

    return new MeetupDetailContext(
        meetup,
        currentUser,
        role,
        myRequest,
        isFull,
        offeredGames,
        creatorDisplayName,
        zipCode,
        creatorAddress);
  }
}

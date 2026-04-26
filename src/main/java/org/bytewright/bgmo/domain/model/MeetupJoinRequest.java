package org.bytewright.bgmo.domain.model;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import org.bytewright.bgmo.domain.model.data.HasUUID;

/**
 * Represents a request to join a meetup, from either a registered or anonymous user.
 *
 * <p>GDPR notes:
 *
 * <ul>
 *   <li>{@code displayName} is shown publicly to all viewers once the organiser confirms the
 *       attendee.
 *   <li>{@code contactInfo} is shown <em>only</em> to the organiser and only after confirmation; it
 *       must be deleted when the event concludes (enforce via a scheduled cleanup job).
 *   <li>{@code anonToken} is a transient browser-session identifier; it is never shown to other
 *       users and should be treated as personal data.
 * </ul>
 */
@Data
@Builder
public class MeetupJoinRequest implements HasUUID {
  private UUID id;
  private UUID meetupId;

  /**
   * The id of the registered user who sent this request. {@code null} for anonymous
   * (non-registered) join requests.
   */
  @Nullable private UUID userId;

  /**
   * The name the requester chose to be shown publicly (once confirmed). Required for both
   * registered and anonymous requesters; for registered users this is pre-filled with {@code
   * RegisteredUser#getName()}.
   */
  private String displayName;

  /**
   * A random UUID generated per browser session for anonymous requesters. Stored in the Vaadin
   * session so the same visitor can detect their own pending request when they navigate back to the
   * page without logging in. {@code null} for registered-user requests.
   */
  @Nullable private UUID anonToken;

  /**
   * How the organiser can reach this person (e.g. Telegram handle, phone, e-mail). Shown
   * <em>exclusively</em> to the event organiser and only after they confirm the attendee. May be
   * {@code null} for registered users who opted not to supply extra contact info.
   */
  @Nullable private String contactInfo;

  @Nullable private String comment;

  private Instant tsCreation;
  @Builder.Default private RequestState requestState = RequestState.OPEN;
}

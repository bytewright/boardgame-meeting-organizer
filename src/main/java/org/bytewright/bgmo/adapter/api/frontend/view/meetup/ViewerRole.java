package org.bytewright.bgmo.adapter.api.frontend.view.meetup;

/**
 * Represents the relationship of the current visitor to a specific meetup event.
 *
 * <p>REGISTERED_PENDING covers both "no request yet" and "OPEN request" — the distinction inside
 * the panel is made by checking {@code ctx.myRequest().isPresent()}.
 */
public enum ViewerRole {
  /** No session token and not logged in. May submit a new anon join request. */
  ANONYMOUS,

  /** Has a valid session token with a matching OPEN join request. */
  ANON_PENDING,

  /**
   * Has a valid session token and the request has been ACCEPTED. Sees the full address. Note: this
   * state is lost when the browser session ends.
   */
  ANON_ACCEPTED,

  /** Logged in, either has no request yet or has an OPEN request. */
  REGISTERED_PENDING,

  /** Logged in and request has been ACCEPTED. Sees the full address. */
  REGISTERED_ACCEPTED,
  /** Logged in and request has been canceled by user. */
  REGISTERED_CANCELED,

  /** The logged-in user is the event creator. Full admin access. */
  ORGANIZER
}

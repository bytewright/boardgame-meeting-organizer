package org.bytewright.bgmo.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SlotDistributionStrategy {
  /**
   * FIFO with wait list, if a slot becomes free (Attendee canceled or rejected), first of waitlist
   * gets the slot
   */
  FIRST_COME_FIRST_SERVE(
      "slot-distribution.strategy.first-come-first-serve",
      "slot-distribution.strategy.first-come-first-serve.helper"),
  /** All requests are shown to organizer to pick and choose or distribute at random */
  REQUEST_APPROVE(
      "slot-distribution.strategy.request-approve",
      "slot-distribution.strategy.request-approve.helper"),
  /** At registration closed deadline chooses random attendees from join-request-pool */
  LOTTERY("slot-distribution.strategy.lottery", "slot-distribution.strategy.lottery.helper");
  private final String messageKey;
  private final String helperTextKey;
}

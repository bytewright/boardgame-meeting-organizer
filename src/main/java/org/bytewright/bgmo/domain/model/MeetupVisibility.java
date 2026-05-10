package org.bytewright.bgmo.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MeetupVisibility {
  PUBLIC("meetup-visibility.strategy.public", "meetup-visibility.strategy.public.helper"),
  WITH_LINK_ONLY(
      "meetup-visibility.strategy.with-link-only",
      "meetup-visibility.strategy.with-link-only.helper");

  private final String messageKey;
  private final String helperTextKey;
}

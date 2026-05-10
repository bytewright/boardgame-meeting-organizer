package org.bytewright.bgmo.domain.service;

import static org.bytewright.bgmo.domain.model.SlotDistributionStrategy.*;

import java.util.List;
import java.util.Locale;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bytewright.bgmo.domain.model.MeetupVisibility;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MeetupVisibilityService {
  private final MessageSource messageSource;

  /** Default should be first element from list */
  public List<MeetupVisibilityStrategyWithLocalization> getAvailableMeetupVisibilityStrategies(
      Locale locale) {
    return List.of(
        getDto(locale, MeetupVisibility.PUBLIC), getDto(locale, MeetupVisibility.WITH_LINK_ONLY));
  }

  private MeetupVisibilityStrategyWithLocalization getDto(
      Locale locale, MeetupVisibility strategy) {
    return MeetupVisibilityStrategyWithLocalization.builder()
        .strategy(strategy)
        .displayName(messageSource.getMessage(strategy.getMessageKey(), null, locale))
        .helpText(messageSource.getMessage(strategy.getHelperTextKey(), null, locale))
        .build();
  }

  @Data
  @Builder
  public static class MeetupVisibilityStrategyWithLocalization {
    private MeetupVisibility strategy;
    private String displayName;
    private String helpText;
  }
}

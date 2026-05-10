package org.bytewright.bgmo.domain.service;

import static org.bytewright.bgmo.domain.model.SlotDistributionStrategy.*;

import java.util.List;
import java.util.Locale;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bytewright.bgmo.domain.model.SlotDistributionStrategy;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MeetupStrategyService {
  private final MessageSource messageSource;

  /** Default should be first element from list */
  public List<SlotDistributionStrategyWithLocalization> getAvailableSlotDistributionStrategies(
      Locale locale) {
    return List.of(
        getDto(locale, FIRST_COME_FIRST_SERVE),
        getDto(locale, REQUEST_APPROVE),
        getDto(locale, LOTTERY));
  }

  private SlotDistributionStrategyWithLocalization getDto(
      Locale locale, SlotDistributionStrategy strategy) {
    return SlotDistributionStrategyWithLocalization.builder()
        .strategy(strategy)
        .displayName(messageSource.getMessage(strategy.getMessageKey(), null, locale))
        .helpText(messageSource.getMessage(strategy.getHelperTextKey(), null, locale))
        .build();
  }

  @Data
  @Builder
  public static class SlotDistributionStrategyWithLocalization {
    private SlotDistributionStrategy strategy;
    private String displayName;
    private String helpText;
  }
}

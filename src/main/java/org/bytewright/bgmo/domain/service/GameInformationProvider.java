package org.bytewright.bgmo.domain.service;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import lombok.Builder;
import org.bytewright.bgmo.domain.model.Game;

public interface GameInformationProvider {

  InputConfig getInputConfig(Locale locale);

  /**
   * Based on userinput (e.g. bgg id) the implementing class should be able to create a Game object.
   */
  Optional<Game.Creation> generateGame(String userInput);

  @Builder
  class InputConfig {
    /** Shown to users in Dropdown */
    private String providerDisplayName;

    /** Shown to users */
    private String providerLogoLink;

    /** Shown to users before they are prompted for input for generateGame method */
    private String inpoutHint;

    /** offline check if a given input is parseable/usable */
    private Function<String, Boolean> inputDataValidator;
  }
}

package org.bytewright.bgmo.adapter.api.frontend.view.meetup.component;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.bytewright.bgmo.adapter.api.frontend.service.MeetupDetailContext;

/**
 * "What's on the table" section. Renders a {@link GameCard} for each offered game. Each card is
 * collapsed by default; clicking expands the full game details.
 *
 * <p>Only added to the page when the meetup has offered games ({@code ctx.offeredGames()} is
 * non-empty).
 */
public class OfferedGamesSection extends VerticalLayout {

  public OfferedGamesSection(MeetupDetailContext ctx) {
    setPadding(false);
    setSpacing(true);

    add(new H3(getTranslation("meetup.offeredGames")));
    ctx.offeredGames().stream().map(GameCard::new).forEach(this::add);
  }
}

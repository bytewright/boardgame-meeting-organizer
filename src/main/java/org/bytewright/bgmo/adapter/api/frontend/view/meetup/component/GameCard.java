package org.bytewright.bgmo.adapter.api.frontend.view.meetup.component;

import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.bytewright.bgmo.domain.model.Game;

/**
 * A collapsible card for a single game offered at a meetup.
 *
 * <p>Collapsed (summary): artwork thumbnail, name, player count.
 *
 * <p>Expanded: adds description, complexity rating, play time estimate, and all associated URLs
 * (including the BoardGameGeek link when a BGG ID is set).
 *
 * <p>Uses the Vaadin {@link Details} component so no custom JS is required.
 */
public class GameCard extends Details {

  public GameCard(Game game) {
    setSummary(buildSummary(game));
    add(buildExpandedContent(game));

    getStyle()
        .set("background", "var(--lumo-contrast-5pct)")
        .set("border-radius", "var(--lumo-border-radius-m)")
        .set("padding", "var(--lumo-space-s)");
    setWidthFull();
  }

  // ── Summary (always visible) ────────────────────────────────────────────────

  private HorizontalLayout buildSummary(Game game) {
    Image img =
        new Image(
            game.getArtworkLink() != null ? game.getArtworkLink() : "images/default-game.png",
            game.getName());
    img.setWidth("60px");
    img.setHeight("60px");
    img.getStyle().set("object-fit", "cover").set("border-radius", "var(--lumo-border-radius-s)");

    Span name = new Span(game.getName());
    name.getStyle().set("font-weight", "bold");

    String playerRange =
        game.getOptimalPlayers() != null
            ? getTranslation(
                "meetup.game.players.optimal",
                game.getMinPlayers(),
                game.getMaxPlayers(),
                game.getOptimalPlayers())
            : getTranslation("meetup.game.players", game.getMinPlayers(), game.getMaxPlayers());

    Span players = new Span("👥 " + playerRange);
    players
        .getStyle()
        .set("font-size", "var(--lumo-font-size-s)")
        .set("color", "var(--lumo-secondary-text-color)");

    VerticalLayout nameAndPlayers = new VerticalLayout(name, players);
    nameAndPlayers.setPadding(false);
    nameAndPlayers.setSpacing(false);

    HorizontalLayout summary = new HorizontalLayout(img, nameAndPlayers);
    summary.setAlignItems(FlexComponent.Alignment.CENTER);
    summary.setSpacing(true);
    return summary;
  }

  // ── Expanded content ────────────────────────────────────────────────────────

  private VerticalLayout buildExpandedContent(Game game) {
    VerticalLayout content = new VerticalLayout();
    content.setPadding(false);
    content.setSpacing(true);

    // Description
    if (game.getDescription() != null && !game.getDescription().isBlank()) {
      Paragraph desc = new Paragraph(game.getDescription());
      desc.getStyle().set("margin", "0");
      content.add(desc);
    }

    // Stats row: complexity + play time
    HorizontalLayout stats = new HorizontalLayout();
    stats.setSpacing(true);

    if (game.getComplexity() != null) {
      Span complexity =
          new Span(
              getTranslation(
                  "meetup.game.complexity", String.format("%.1f", game.getComplexity())));
      complexity
          .getStyle()
          .set("font-size", "var(--lumo-font-size-s)")
          .set("background", "var(--lumo-contrast-10pct)")
          .set("padding", "2px 8px")
          .set("border-radius", "var(--lumo-border-radius-s)");
      stats.add(complexity);
    }

    if (game.getPlayTimeMinutesPerPlayer() != null) {
      Span playTime =
          new Span(getTranslation("meetup.game.playTime", game.getPlayTimeMinutesPerPlayer()));
      playTime
          .getStyle()
          .set("font-size", "var(--lumo-font-size-s)")
          .set("background", "var(--lumo-contrast-10pct)")
          .set("padding", "2px 8px")
          .set("border-radius", "var(--lumo-border-radius-s)");
      stats.add(playTime);
    }

    if (!stats.getChildren().findAny().isEmpty()) {
      content.add(stats);
    }

    // Links section
    VerticalLayout links = new VerticalLayout();
    links.setPadding(false);
    links.setSpacing(false);

    if (game.getBggId() != null) {
      Anchor bggLink =
          new Anchor(
              "https://boardgamegeek.com/boardgame/" + game.getBggId(),
              getTranslation("meetup.game.bgg"));
      bggLink.setTarget("_blank");
      bggLink.getStyle().set("font-size", "var(--lumo-font-size-s)");
      links.add(bggLink);
    }

    for (var url : game.getUrls()) {
      Anchor userLink = new Anchor(url.url(), url.displayText());
      userLink.setTarget("_blank");
      userLink.getStyle().set("font-size", "var(--lumo-font-size-s)");
      links.add(userLink);
    }

    if (links.getChildren().findAny().isPresent()) {
      Span linksLabel = new Span(getTranslation("meetup.game.links"));
      linksLabel
          .getStyle()
          .set("font-size", "var(--lumo-font-size-xs)")
          .set("color", "var(--lumo-secondary-text-color)")
          .set("font-weight", "bold");
      links.addComponentAsFirst(linksLabel);
      content.add(links);
    }

    return content;
  }
}

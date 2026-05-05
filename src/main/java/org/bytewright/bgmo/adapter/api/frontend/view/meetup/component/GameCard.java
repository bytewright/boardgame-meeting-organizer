package org.bytewright.bgmo.adapter.api.frontend.view.meetup.component;

import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.bytewright.bgmo.domain.model.Game;

/**
 * A collapsible card for a single game offered at a meetup.
 *
 * <p>Collapsed (summary): artwork thumbnail, name, player count.
 *
 * <p>Expanded: adds description, owner notes, complexity rating, play time estimate, tags, and all
 * associated URLs (including the BoardGameGeek link when a BGG ID is set).
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

    // Stats row: complexity + play time
    HorizontalLayout stats = new HorizontalLayout();
    stats.setSpacing(true);

    if (game.getComplexity() != null) {
      Span complexity =
          new Span(
              getTranslation(
                  "meetup.game.complexity", String.format("%.1f", game.getComplexity())));
      styleStatBadge(complexity);
      stats.add(complexity);
    }

    if (game.getPlayTimeMinutesPerPlayer() != null) {
      Span playTime =
          new Span(getTranslation("meetup.game.playTime", game.getPlayTimeMinutesPerPlayer()));
      styleStatBadge(playTime);
      stats.add(playTime);
    }

    if (stats.getChildren().findAny().isPresent()) {
      content.add(stats);
    }

    // Tags row – only rendered when at least one tag exists
    if (game.getTags() != null && !game.getTags().isEmpty()) {
      Span tagsLabel = new Span(getTranslation("meetup.game.tags"));
      tagsLabel
          .getStyle()
          .set("font-size", "var(--lumo-font-size-xs)")
          .set("color", "var(--lumo-secondary-text-color)")
          .set("font-weight", "bold");

      FlexLayout tagsRow = new FlexLayout();
      tagsRow.setFlexWrap(FlexLayout.FlexWrap.WRAP);
      tagsRow.getStyle().set("gap", "var(--lumo-space-xs)");

      for (String tag : game.getTags()) {
        Span chip = new Span(tag);
        chip.getStyle()
            .set("font-size", "var(--lumo-font-size-xs)")
            .set("background", "var(--lumo-primary-color-10pct)")
            .set("border-radius", "var(--lumo-border-radius-l)")
            .set("padding", "2px 8px");
        tagsRow.add(chip);
      }

      VerticalLayout tagsBlock = new VerticalLayout(tagsLabel, tagsRow);
      tagsBlock.setPadding(false);
      tagsBlock.setSpacing(false);
      content.add(tagsBlock);
    }
    // Description
    if (game.getDescription() != null && !game.getDescription().isBlank()) {

      Div descDiv = new Div();
      descDiv.setText(game.getDescription());
      descDiv
          .getStyle()
          .set("margin", "0")
          .set("white-space", "pre-wrap") // honours \n, wraps long lines
          .set("word-break", "break-word") // no overflow on long unbroken strings
          .set("font-family", "inherit");
      content.add(descDiv);
    }

    // Owner notes – visually distinct from the public description
    if (game.getNotes() != null && !game.getNotes().isBlank()) {
      Span notesLabel = new Span(getTranslation("meetup.game.notes"));
      notesLabel
          .getStyle()
          .set("font-size", "var(--lumo-font-size-xs)")
          .set("color", "var(--lumo-secondary-text-color)")
          .set("font-weight", "bold");
      Div notesDiv = new Div();
      notesDiv.setText(game.getNotes());
      notesDiv
          .getStyle()
          .set("white-space", "pre-wrap") // honours \n, wraps long lines
          .set("word-break", "break-word") // no overflow on long unbroken strings
          .set("font-family", "inherit")
          .set("font-style", "italic")
          .set("color", "var(--lumo-secondary-text-color)");

      VerticalLayout notesBlock = new VerticalLayout(notesLabel, notesDiv);
      notesBlock.setPadding(false);
      notesBlock.setSpacing(false);
      notesBlock
          .getStyle()
          .set("background", "var(--lumo-contrast-5pct)")
          .set("border-radius", "var(--lumo-border-radius-s)")
          .set("padding", "var(--lumo-space-xs) var(--lumo-space-s)");

      content.add(notesBlock);
    }

    // Links section
    VerticalLayout links = new VerticalLayout();
    links.setPadding(false);
    links.setSpacing(false);

    String bggUrl = "https://boardgamegeek.com/boardgame/" + game.getBggId();
    boolean isBggUrlPresent = false;
    for (Game.UserLink userLink : game.getUrls()) {
      // Prefer the display text; fall back to the raw URL if blank.
      String anchorText =
          (userLink.displayText() != null && !userLink.displayText().isBlank())
              ? userLink.displayText()
              : userLink.url();
      Anchor anchor = new Anchor(userLink.url(), anchorText);
      if (userLink.url().equals(bggUrl)) isBggUrlPresent = true;
      anchor.setTarget("_blank");
      anchor.getStyle().set("font-size", "var(--lumo-font-size-s)");
      links.add(anchor);
    }
    if (!isBggUrlPresent && game.getBggId() != null) {
      Anchor bggLink = new Anchor(bggUrl, getTranslation("meetup.game.bgg"));
      bggLink.setTarget("_blank");
      bggLink.getStyle().set("font-size", "var(--lumo-font-size-s)");
      links.add(bggLink);
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

  private void styleStatBadge(Span span) {
    span.getStyle()
        .set("font-size", "var(--lumo-font-size-s)")
        .set("background", "var(--lumo-contrast-10pct)")
        .set("padding", "2px 8px")
        .set("border-radius", "var(--lumo-border-radius-s)");
  }
}

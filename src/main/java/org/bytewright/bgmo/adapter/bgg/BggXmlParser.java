package org.bytewright.bgmo.adapter.bgg;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.Game;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Parses the XML response from the BGG Thing API (v2) into a {@link Game.Creation} object.
 *
 * <p>BGG endpoint: {@code /thing?id={bggId}&stats=1}
 *
 * <p>Extracted fields:
 *
 * <ul>
 *   <li>Primary name
 *   <li>Min/max player counts
 *   <li>Optimal player count — derived from the community "suggested_numplayers" poll (the
 *       numplayers value with the highest "Best" vote count)
 *   <li>Description (raw text, BGG HTML-encodes special chars)
 *   <li>Complexity — BGG's {@code averageweight} (1.0–5.0 scale)
 *   <li>Artwork link — full-resolution image URL
 *   <li>BGG game page URL added to the {@code urls} list
 * </ul>
 *
 * <p>Note: {@code playTimeMinutesPerPlayer} is intentionally left null because BGG only provides
 * total play time, not a per-player figure.
 */
@Slf4j
@Component
class BggXmlParser {

  private static final String BGG_GAME_URL_PREFIX = "https://boardgamegeek.com/boardgame/";
  private static final String POLL_SUGGESTED_NUMPLAYERS = "suggested_numplayers";

  Optional<Game.Creation> parseGameCreation(String xml, long bggId) {
    try {
      Document doc = parseXmlSafely(xml);

      NodeList items = doc.getElementsByTagName("item");
      if (items.getLength() == 0) {
        log.warn("BGG API returned no items for bggId={}", bggId);
        return Optional.empty();
      }

      Element item = (Element) items.item(0);

      String name = extractPrimaryName(item);
      if (name == null || name.isBlank()) {
        log.warn("BGG API returned an item without a primary name for bggId={}", bggId);
        return Optional.empty();
      }

      int minPlayers = extractIntValueAttr(item, "minplayers", 1);
      int maxPlayers = extractIntValueAttr(item, "maxplayers", 1);
      String description = extractTextContent(item, "description");
      String artworkLink = extractTextContent(item, "image");
      Double complexity = extractComplexity(item);
      Integer optimalPlayers = extractOptimalPlayers(item, minPlayers, maxPlayers);

      List<String> urls = new ArrayList<>();
      urls.add(BGG_GAME_URL_PREFIX + bggId);

      return Optional.of(
          Game.Creation.builder()
              .name(name)
              .bggId(bggId)
              .minPlayers(minPlayers)
              .maxPlayers(maxPlayers)
              .description(nullIfBlank(description))
              .optimalPlayers(optimalPlayers)
              .complexity(complexity)
              .artworkLink(nullIfBlank(artworkLink != null ? artworkLink.strip() : null))
              .urls(urls)
              .build());

    } catch (Exception e) {
      log.error("Failed to parse BGG XML response for bggId={}", bggId, e);
      return Optional.empty();
    }
  }

  // --- XML helpers ---

  /** XXE-safe document builder. */
  private Document parseXmlSafely(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    DocumentBuilder builder = factory.newDocumentBuilder();
    return builder.parse(new InputSource(new StringReader(xml)));
  }

  /** Returns the {@code value} attribute of the first element matching {@code tagName}. */
  private int extractIntValueAttr(Element item, String tagName, int fallback) {
    NodeList nodes = item.getElementsByTagName(tagName);
    if (nodes.getLength() > 0) {
      String raw = ((Element) nodes.item(0)).getAttribute("value");
      try {
        return Integer.parseInt(raw);
      } catch (NumberFormatException e) {
        log.debug("Could not parse integer '{}' for tag <{}>", raw, tagName);
      }
    }
    return fallback;
  }

  /** Returns the text content of the first element matching {@code tagName}. */
  private String extractTextContent(Element item, String tagName) {
    NodeList nodes = item.getElementsByTagName(tagName);
    return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
  }

  /** Finds the {@code <name type="primary">} element and returns its {@code value} attribute. */
  private String extractPrimaryName(Element item) {
    NodeList names = item.getElementsByTagName("name");
    for (int i = 0; i < names.getLength(); i++) {
      Element el = (Element) names.item(i);
      if ("primary".equals(el.getAttribute("type"))) {
        return el.getAttribute("value");
      }
    }
    return null;
  }

  /**
   * Reads {@code averageweight} from the {@code <statistics>} block. Returns null if weight is 0
   * (meaning no votes yet).
   */
  private Double extractComplexity(Element item) {
    NodeList nodes = item.getElementsByTagName("averageweight");
    if (nodes.getLength() > 0) {
      String raw = ((Element) nodes.item(0)).getAttribute("value");
      try {
        double weight = Double.parseDouble(raw);
        return weight > 0.0 ? weight : null;
      } catch (NumberFormatException e) {
        log.debug("Could not parse averageweight: '{}'", raw);
      }
    }
    return null;
  }

  /**
   * Determines the optimal player count from the BGG community poll. Finds the numplayers value
   * (within [minPlayers, maxPlayers]) with the highest "Best" vote count. Returns null if no poll
   * data is present or no votes were cast.
   */
  private Integer extractOptimalPlayers(Element item, int minPlayers, int maxPlayers) {
    NodeList polls = item.getElementsByTagName("poll");
    for (int i = 0; i < polls.getLength(); i++) {
      Element poll = (Element) polls.item(i);
      if (POLL_SUGGESTED_NUMPLAYERS.equals(poll.getAttribute("name"))) {
        return findHighestBestVoteCount(poll, minPlayers, maxPlayers);
      }
    }
    return null;
  }

  private Integer findHighestBestVoteCount(Element poll, int minPlayers, int maxPlayers) {
    NodeList resultGroups = poll.getElementsByTagName("results");
    int bestPlayerCount = -1;
    int bestVotes = 0; // require at least 1 vote

    for (int i = 0; i < resultGroups.getLength(); i++) {
      Element results = (Element) resultGroups.item(i);
      String numPlayersStr = results.getAttribute("numplayers");

      // BGG includes an open-ended "4+" entry for high player counts — skip it
      if (numPlayersStr.contains("+")) {
        continue;
      }

      int numPlayers;
      try {
        numPlayers = Integer.parseInt(numPlayersStr);
      } catch (NumberFormatException e) {
        continue;
      }

      if (numPlayers < minPlayers || numPlayers > maxPlayers) {
        continue;
      }

      NodeList resultNodes = results.getElementsByTagName("result");
      for (int j = 0; j < resultNodes.getLength(); j++) {
        Element result = (Element) resultNodes.item(j);
        if ("Best".equals(result.getAttribute("value"))) {
          try {
            int votes = Integer.parseInt(result.getAttribute("numvotes"));
            if (votes > bestVotes) {
              bestVotes = votes;
              bestPlayerCount = numPlayers;
            }
          } catch (NumberFormatException ignored) {
            // malformed vote count — skip
          }
        }
      }
    }

    return bestPlayerCount > 0 ? bestPlayerCount : null;
  }

  private String nullIfBlank(String value) {
    return (value == null || value.isBlank()) ? null : value;
  }
}

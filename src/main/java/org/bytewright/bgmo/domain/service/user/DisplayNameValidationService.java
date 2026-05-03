package org.bytewright.bgmo.domain.service.user;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.user.ValidationResult;
import org.bytewright.bgmo.domain.service.BgmoProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisplayNameValidationService implements InitializingBean {
  private static final Map<Character, Character> LEET_MAP =
      Map.ofEntries(
          Map.entry('0', 'o'),
          Map.entry('1', 'i'),
          Map.entry('2', 'z'),
          Map.entry('3', 'e'),
          Map.entry('4', 'a'),
          Map.entry('5', 's'),
          Map.entry('6', 'g'),
          Map.entry('7', 't'),
          Map.entry('8', 'b'),
          Map.entry('9', 'g'),
          Map.entry('@', 'a'),
          Map.entry('$', 's'),
          Map.entry('!', 'i'),
          Map.entry('+', 't'),
          Map.entry('|', 'i'));
  private final BgmoProperties bgmoProperties;
  private final Set<String> blocklist = new HashSet<>();

  @Override
  public void afterPropertiesSet() throws Exception {
    if (bgmoProperties.getProfanityFilterListPath() == null
        || !StringUtils.hasText(bgmoProperties.getProfanityFilterListPath())) {
      log.error("Profanity list app property is not set!");
      return;
    }
    // todo fix loading from classpath
    if (true) return;
    Path path = Path.of(bgmoProperties.getProfanityFilterListPath());
    if (!Files.exists(path)) {
      log.error("Can't find File with path: {}", path.toAbsolutePath());
      return;
    }
    try {
      List<String> strings = Files.readAllLines(path);
      blocklist.addAll(strings);
    } catch (Exception e) {
      log.error(
          "Found file with path '{}' but its not readable by application",
          path.toAbsolutePath(),
          e);
    }
  }

  /** Normalize: lowercase, leet-decode, strip non-alpha */
  private String normalize(String input) {
    // Lowercase + decompose accented characters (ä→a, ö→o, ü→u, etc.)
    String decomposed = Normalizer.normalize(input.toLowerCase(), Normalizer.Form.NFD);

    // Leet-decode and strip anything non-alpha
    StringBuilder sb = new StringBuilder(decomposed.length());
    for (char c : decomposed.toCharArray()) {
      if (LEET_MAP.containsKey(c)) {
        sb.append(LEET_MAP.get(c));
      } else if (Character.isLetter(c) && Character.getType(c) != Character.NON_SPACING_MARK) {
        sb.append(c);
      }
      // spaces, punctuation, combining diacritics → dropped
    }

    // Collapse repeated characters: "shhhhit" → "shit"
    return sb.toString().replaceAll("(.)\\1{2,}", "$1");
  }

  public ValidationResult validate(String displayName) {
    if (displayName == null || displayName.isBlank()) return ValidationResult.fail("empty");
    if (displayName.length() < 2 || displayName.length() > 64)
      return ValidationResult.fail("length");

    String normalized = normalize(displayName);
    boolean hasProfanity =
        blocklist.stream().anyMatch(s -> normalized.startsWith(s) || normalized.endsWith(s));
    if (hasProfanity) return ValidationResult.fail("profanity");

    return ValidationResult.ok();
  }
}

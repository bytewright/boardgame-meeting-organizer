package org.bytewright.bgmo.adapter.bgg;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.Game;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Low-level HTTP client for the BoardGameGeek XML API v2.
 *
 * <p>BGG's API is eventually consistent: a first request for an uncached game returns {@code 202
 * Accepted} while the result is being computed. This client retries transparently up to {@value
 * #MAX_RETRIES} times with a {@value #RETRY_DELAY_MS}ms delay.
 *
 * <p>The field name {@code bggRestClient} intentionally matches the bean name defined in {@link
 * BggConfiguration} so Spring resolves the correct bean by name without an explicit
 * {@code @Qualifier}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class BggApiClient {

  static final int MAX_RETRIES = 3;
  static final long RETRY_DELAY_MS = 2_000;

  // Field name matches the bean name from BggConfiguration — Spring resolves by name on ambiguity
  private final RestClient bggRestClient;

  /**
   * Fetches game data for the given BGG ID and parses it into a {@link Game.Creation}.
   *
   * @param bggId numeric BoardGameGeek game ID
   * @return parsed creation object, or empty if the game cannot be retrieved or parsed
   */
  Optional<String> fetchGame(long bggId) {
    for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
      try {
        ResponseEntity<String> response =
            bggRestClient
                .get()
                .uri("/thing?id={id}&stats=1", bggId)
                .retrieve()
                .toEntity(String.class);

        int statusCode = response.getStatusCode().value();

        if (statusCode == 202) {
          log.info(
              "BGG returned 202 Accepted for bggId={} (attempt {}/{}), retrying in {}ms",
              bggId,
              attempt,
              MAX_RETRIES,
              RETRY_DELAY_MS);
          if (attempt < MAX_RETRIES) {
            Thread.sleep(RETRY_DELAY_MS);
          }
          continue;
        }

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
          return Optional.of(response.getBody());
        }

        log.warn("Unexpected HTTP {} from BGG API for bggId={}", statusCode, bggId);
        return Optional.empty();

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("Interrupted while waiting for BGG API retry (bggId={})", bggId);
        return Optional.empty();
      } catch (Exception e) {
        log.error("Error calling BGG API for bggId={}", bggId, e);
        return Optional.empty();
      }
    }

    log.warn("BGG API kept returning 202 after {} attempts for bggId={}", MAX_RETRIES, bggId);
    return Optional.empty();
  }
}

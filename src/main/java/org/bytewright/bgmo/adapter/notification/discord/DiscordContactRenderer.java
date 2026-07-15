package org.bytewright.bgmo.adapter.notification.discord;

import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactOption;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;

// @Component
@RequiredArgsConstructor
public class DiscordContactRenderer {

  /**
   * @return a Discord mention like {@code <@123456789012345678>} if the user has linked a Discord
   *     account, otherwise a plain-text fallback using their display name (not clickable, but still
   *     readable — better than a broken link).
   */
  public String render(RegisteredUser user) {
    // ContactOption contactOption = user.resolvePrimaryContact().orElseThrow();
    Optional<ContactInfo.DiscordContact> userDiscordContact =
        user.getContactOptions().stream()
            .map(ContactOption::getContactInfo)
            .flatMap(
                contactInfo ->
                    switch (contactInfo) {
                      case ContactInfo.DiscordContact discordContact -> Stream.of(discordContact);
                      default -> Stream.empty();
                    })
            .findAny();
    return userDiscordContact.map(this::renderMention).orElseGet(user::getDisplayName);
  }

  private String renderMention(ContactInfo.DiscordContact discordContact) {
    return "<@" + discordContact.userId() + ">";
  }
}

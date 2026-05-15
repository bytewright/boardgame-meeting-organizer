package org.bytewright.bgmo.domain.model.notification;

import java.util.List;
import java.util.Optional;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;

/**
 * @param type the messenger type being linked
 * @param verificationCode the one-time code the user must send to the bot
 * @param botHandle the bot's username/handle the user must search for
 * @param botDeepLink direct-open URL if the platform supports it (e.g. {@code https://t.me/…} for
 *     Telegram); empty for messengers without a universal deep-link scheme
 * @param steps ordered tutorial steps shown in the dialog; may be empty
 */
public record MessengerLinkContext(
    ContactInfoType type,
    String verificationCode,
    String botHandle,
    Optional<String> botDeepLink,
    List<VerificationStep> steps) {}

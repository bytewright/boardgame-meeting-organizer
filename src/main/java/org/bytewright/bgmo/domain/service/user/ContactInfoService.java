package org.bytewright.bgmo.domain.service.user;

import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ContactInfoService {

  // Simple email regex - covers 99% of common cases
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

  // Telegram handles: 5-32 chars, alphanumeric and underscores
  private static final Pattern TELEGRAM_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{5,32}$");
  private final RegisteredUserDao userDao;

  public boolean validate(ContactInfo contactInfo) {
    return switch (contactInfo) {
      case ContactInfo.AddressContact addressContact ->
          validateAddress(addressContact.street(), addressContact.zipCode(), addressContact.city());
      case ContactInfo.EmailContact emailContact -> validateEmail(emailContact.email());
      case ContactInfo.PhoneContact phoneContact -> validatePhone(phoneContact.phoneNr());
      case ContactInfo.SignalContact signalContact -> validateSignal(signalContact.signalHandle());
      case ContactInfo.TelegramContact telegramContact -> validateTelegram(telegramContact);
      case ContactInfo.DiscordContact discordContact -> validateDiscord(discordContact);
    };
  }

  private boolean validateDiscord(ContactInfo.DiscordContact contact) {
    return StringUtils.hasText(contact.username()) && contact.userId() > 0;
  }

  public boolean validateEmail(String email) {
    return StringUtils.hasText(email) && EMAIL_PATTERN.matcher(email).matches();
  }

  public boolean validateTelegram(ContactInfo.TelegramContact telegramContact) {
    if (!StringUtils.hasText(telegramContact.username())) return false;
    // Strip leading @ if user entered it
    String handleRaw = telegramContact.username();
    String handle = handleRaw.startsWith("@") ? handleRaw.substring(1) : handleRaw;
    return TELEGRAM_PATTERN.matcher(handle).matches();
  }

  public boolean validateSignal(String signalHandle) {
    // Signal handles are similar to Telegram but allow periods
    return StringUtils.hasText(signalHandle) && signalHandle.length() >= 3;
  }

  public boolean validatePhone(String phoneNr) {
    if (!StringUtils.hasText(phoneNr)) return false;

    // Remove common formatting characters (spaces, dashes, slashes)
    String cleanNumber = phoneNr.replaceAll("[\\s\\-/]", "");

    // Check for German local format (starts with 0, then digits)
    // Minimum 10 digits (e.g., 030 1234567)
    return cleanNumber.startsWith("0") && cleanNumber.matches("\\d{10,15}");
  }

  public boolean validateAddress(String street, String zipCode, String city) {
    if (!StringUtils.hasText(street)
        || !StringUtils.hasText(city)
        || !StringUtils.hasText(zipCode)) {
      return false;
    }

    // German PLZ (Postleitzahl) is exactly 5 digits
    return zipCode.matches("\\d{5}");
  }
}

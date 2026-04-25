package org.bytewright.bgmo.domain.service.user;

import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.springframework.stereotype.Service;

@Service
public class ContactInfoValidationService {
  public boolean validate(ContactInfo contactInfo) {
    return switch (contactInfo) {
      case ContactInfo.AddressContact addressContact ->
          validateAddress(addressContact.street(), addressContact.zipCode(), addressContact.city());
      case ContactInfo.EmailContact emailContact -> validateEmail(emailContact.email());
      case ContactInfo.PhoneContact phoneContact -> validatePhone(phoneContact.phoneNr());
      case ContactInfo.SignalContact signalContact -> validateSignal(signalContact.signalHandle());
      case ContactInfo.TelegramContact telegramContact ->
          validateTelegram(telegramContact.chatId());
    };
  }

  public boolean validateEmail(String email) {
    return true;
  }

  public boolean validateTelegram(String chatId) {
    return true;
  }

  public boolean validateSignal(String signalHandle) {
    return true;
  }

  public boolean validatePhone(String phoneNr) {
    return true;
  }

  public boolean validateAddress(String street, String zipCode, String city) {
    return true;
  }
}

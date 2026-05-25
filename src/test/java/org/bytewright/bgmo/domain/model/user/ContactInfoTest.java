package org.bytewright.bgmo.domain.model.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.bytewright.bgmo.domain.service.JsonMapperFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("ContactInfo JSON serialization/deserialization")
class ContactInfoTest {

  private final JsonMapper mapper = JsonMapperFactory.unRedactedMapper();

  // -------------------------------------------------------------------------
  // Direct (concrete-type) round-trip tests
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("TelegramContact")
  class TelegramContactTests {

    @Test
    @DisplayName("round-trips through JSON preserving all fields")
    void testSerializationTelegram() throws Exception {
      // Arrange
      ContactInfo.TelegramContact contact =
          ContactInfo.TelegramContact.builder()
              .chatId("123456")
              .telegramUsername("SomeName")
              .build();

      // Act
      String json = mapper.writeValueAsString(contact);
      ContactInfo.TelegramContact deserialized =
          mapper.readValue(json, ContactInfo.TelegramContact.class);

      // Assert
      assertThat(deserialized)
          .returns(contact.chatId(), ContactInfo.TelegramContact::chatId)
          .returns(contact.username(), ContactInfo.TelegramContact::username);
    }
  }

  @Nested
  @DisplayName("EmailContact")
  class EmailContactTests {

    @Test
    @DisplayName("round-trips through JSON preserving all fields")
    void testSerializationEmail() throws Exception {
      // Arrange
      ContactInfo.EmailContact contact =
          ContactInfo.EmailContact.builder().email("user@example.com").build();

      // Act
      String json = mapper.writeValueAsString(contact);
      ContactInfo.EmailContact deserialized =
          mapper.readValue(json, ContactInfo.EmailContact.class);

      // Assert
      assertThat(deserialized).returns(contact.email(), ContactInfo.EmailContact::email);
    }
  }

  @Nested
  @DisplayName("PhoneContact")
  class PhoneContactTests {

    @Test
    @DisplayName("round-trips through JSON preserving all fields")
    void testSerializationPhone() throws Exception {
      // Arrange
      ContactInfo.PhoneContact contact =
          ContactInfo.PhoneContact.builder().phoneNr("+491701234567").build();

      // Act
      String json = mapper.writeValueAsString(contact);
      ContactInfo.PhoneContact deserialized =
          mapper.readValue(json, ContactInfo.PhoneContact.class);

      // Assert
      assertThat(deserialized).returns(contact.phoneNr(), ContactInfo.PhoneContact::phoneNr);
    }
  }

  @Nested
  @DisplayName("SignalContact")
  class SignalContactTests {

    @Test
    @DisplayName("round-trips through JSON preserving all fields")
    void testSerializationSignal() throws Exception {
      // Arrange
      ContactInfo.SignalContact contact =
          ContactInfo.SignalContact.builder().signalHandle("+491709876543").build();

      // Act
      String json = mapper.writeValueAsString(contact);
      ContactInfo.SignalContact deserialized =
          mapper.readValue(json, ContactInfo.SignalContact.class);

      // Assert
      assertThat(deserialized)
          .returns(contact.signalHandle(), ContactInfo.SignalContact::signalHandle);
    }
  }

  @Nested
  @DisplayName("AddressContact")
  class AddressContactTests {

    @Test
    @DisplayName("round-trips through JSON preserving all fields")
    void testSerializationAddress() throws Exception {
      // Arrange
      ContactInfo.AddressContact contact =
          ContactInfo.AddressContact.builder()
              .nameOnBell("Jane Doe")
              .street("Musterstraße 1")
              .zipCode("07743")
              .city("Jena")
              .comment("2nd floor")
              .build();

      // Act
      String json = mapper.writeValueAsString(contact);
      ContactInfo.AddressContact deserialized =
          mapper.readValue(json, ContactInfo.AddressContact.class);

      // Assert
      assertThat(deserialized)
          .returns(contact.nameOnBell(), ContactInfo.AddressContact::nameOnBell)
          .returns(contact.street(), ContactInfo.AddressContact::street)
          .returns(contact.zipCode(), ContactInfo.AddressContact::zipCode)
          .returns(contact.city(), ContactInfo.AddressContact::city)
          .returns(contact.comment(), ContactInfo.AddressContact::comment);
    }
  }

  @Nested
  @DisplayName("Polymorphic deserialization via ContactInfo interface")
  class PolymorphicTests {

    @Test
    @DisplayName("deserializes EmailContact from ContactInfo reference")
    void testPolymorphicEmail() throws Exception {
      // Arrange
      ContactInfo.EmailContact original =
          ContactInfo.EmailContact.builder().email("poly@example.com").build();
      String json = mapper.writeValueAsString(original);

      // Act
      ContactInfo deserialized = mapper.readValue(json, ContactInfo.class);

      // Assert
      assertThat(deserialized)
          .isInstanceOf(ContactInfo.EmailContact.class)
          .returns(ContactInfoType.EMAIL, ContactInfo::type);
    }

    @Test
    @DisplayName("deserializes TelegramContact from ContactInfo reference")
    void testPolymorphicTelegram() throws Exception {
      // Arrange
      ContactInfo.TelegramContact original =
          ContactInfo.TelegramContact.builder().chatId("789").telegramUsername("polyUser").build();
      String json = mapper.writeValueAsString(original);

      // Act
      ContactInfo deserialized = mapper.readValue(json, ContactInfo.class);

      // Assert
      assertThat(deserialized)
          .isInstanceOf(ContactInfo.TelegramContact.class)
          .returns(ContactInfoType.TELEGRAM, ContactInfo::type);
    }
  }
}

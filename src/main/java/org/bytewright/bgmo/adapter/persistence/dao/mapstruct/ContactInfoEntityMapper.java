package org.bytewright.bgmo.adapter.persistence.dao.mapstruct;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.persistence.dao.BaseEntityMapper;
import org.bytewright.bgmo.adapter.persistence.dao.BaseMapperConfig;
import org.bytewright.bgmo.adapter.persistence.entity.user.ContactInfoEntity;
import org.bytewright.bgmo.adapter.persistence.entity.user.RegisteredUserEntity;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.service.data.ModelDao;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Transactional
@Mapper(config = BaseMapperConfig.class)
@Setter(onMethod_ = {@Autowired})
public abstract class ContactInfoEntityMapper
    extends BaseEntityMapper<ContactInfo, ContactInfoEntity> implements ModelDao<ContactInfo> {
  private ObjectMapper objectMapper;

  @Override
  public void updateEntity(ContactInfoEntity currentEntity, ContactInfo model) {
    currentEntity.setType(model.type());
    currentEntity.setUser(
        getEntityManager().getReference(RegisteredUserEntity.class, model.userId()));
    currentEntity.setId(model.getId());
    currentEntity.setJsonData(toJson(model));
  }

  @SneakyThrows
  private String toJson(ContactInfo model) {
    Object data =
        switch (model) {
          case ContactInfo.AddressContact address -> AddressDto.from(address);
          case ContactInfo.EmailContact emailContact -> emailContact.email();
          case ContactInfo.PhoneContact phoneContact -> phoneContact.phoneNr();
          case ContactInfo.SignalContact signalContact -> signalContact.signalHandle();
          case ContactInfo.TelegramContact telegramContact -> telegramContact.telegramHandle();
        };
    return objectMapper.writeValueAsString(data);
  }

  @SneakyThrows
  @Override
  public ContactInfo toDto(ContactInfoEntity entity) {
    return switch (entity.getType()) {
      case EMAIL ->
          ContactInfo.EmailContact.builder()
              .id(entity.getId())
              .userId(entity.getUser().id())
              .email(objectMapper.readValue(entity.getJsonData(), String.class))
              .build();
      case TELEGRAM ->
          ContactInfo.TelegramContact.builder()
              .id(entity.getId())
              .userId(entity.getUser().id())
              .telegramHandle(objectMapper.readValue(entity.getJsonData(), String.class))
              .build();
      case SIGNAL ->
          ContactInfo.SignalContact.builder()
              .id(entity.getId())
              .userId(entity.getUser().id())
              .signalHandle(objectMapper.readValue(entity.getJsonData(), String.class))
              .build();
      case PHONE ->
          ContactInfo.PhoneContact.builder()
              .id(entity.getId())
              .userId(entity.getUser().id())
              .phoneNr(objectMapper.readValue(entity.getJsonData(), String.class))
              .build();
      case ADDRESS -> {
        AddressDto addressDto = objectMapper.readValue(entity.getJsonData(), AddressDto.class);
        yield ContactInfo.AddressContact.builder()
            .id(entity.getId())
            .userId(entity.getUser().id())
            .nameOnBell(addressDto.nameOnBell())
            .street(addressDto.street())
            .zipCode(addressDto.zipCode())
            .city(addressDto.city())
            .comment(addressDto.comment())
            .build();
      }
    };
  }

  @Override
  protected Class<ContactInfoEntity> getEntityClass() {
    return ContactInfoEntity.class;
  }

  private record AddressDto(
      String nameOnBell, String street, String zipCode, String city, String comment) {
    public static AddressDto from(ContactInfo.AddressContact address) {
      return new AddressDto(
          address.nameOnBell(),
          address.street(),
          address.zipCode(),
          address.city(),
          address.comment());
    }
  }
}

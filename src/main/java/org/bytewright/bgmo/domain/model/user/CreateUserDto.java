package org.bytewright.bgmo.domain.model.user;

import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class CreateUserDto {
  private String displayName;
  private String loginName;
  private String password;
  @Nullable private String email;
  @Nullable private String signalHandle;
  @Nullable private String telegramHandle;
}

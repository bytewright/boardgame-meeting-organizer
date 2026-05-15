package org.bytewright.bgmo.domain.model.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ExceptionWithMessageKey extends Exception {
  private final String messageKey;
}

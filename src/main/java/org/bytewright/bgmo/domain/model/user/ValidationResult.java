package org.bytewright.bgmo.domain.model.user;

public sealed interface ValidationResult permits ValidationResult.Failed, ValidationResult.Success {
  boolean isPassed();

  static ValidationResult fail(String reason) {
    return new Failed(reason);
  }

  static ValidationResult ok() {
    return new Success();
  }

  record Failed(String reason) implements ValidationResult {
    @Override
    public boolean isPassed() {
      return false;
    }
  }

  record Success() implements ValidationResult {
    @Override
    public boolean isPassed() {
      return true;
    }
  }
}

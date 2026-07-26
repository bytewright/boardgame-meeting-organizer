package org.bytewright.bgmo.domain.model;

public sealed interface JoinRequestResult
    permits JoinRequestResult.Success, JoinRequestResult.Duplicate, JoinRequestResult.Failed {
  JoinRequestResult.Success SUCCESS = new Success();
  JoinRequestResult.Duplicate DUPLICATE = new Duplicate();

  record Success() implements JoinRequestResult {}

  record Duplicate() implements JoinRequestResult {}

  record Failed(String reason) implements JoinRequestResult {}
}

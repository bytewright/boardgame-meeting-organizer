package org.bytewright.bgmo.domain.model.event;

import java.util.UUID;

public sealed interface ModelUpdatedEvents
    permits ModelUpdatedEvents.JoinRequestApproved,
        ModelUpdatedEvents.JoinRequestCanceled,
        ModelUpdatedEvents.JoinRequestCreated,
        ModelUpdatedEvents.JoinRequestWaitlisted,
        ModelUpdatedEvents.MeetupCanceled,
        ModelUpdatedEvents.MeetupCreated,
        ModelUpdatedEvents.MeetupRescheduled,
        ModelUpdatedEvents.UserCreated,
        ModelUpdatedEvents.UserVerified {
  UUID id();

  TargetModel target();

  record UserCreated(UUID id) implements ModelUpdatedEvents {
    @Override
    public TargetModel target() {
      return TargetModel.USER;
    }
  }

  record UserVerified(UUID id) implements ModelUpdatedEvents {
    @Override
    public TargetModel target() {
      return TargetModel.USER;
    }
  }

  record JoinRequestCreated(UUID id) implements ModelUpdatedEvents {
    @Override
    public TargetModel target() {
      return TargetModel.JOIN_REQUEST;
    }
  }

  record JoinRequestCanceled(UUID id) implements ModelUpdatedEvents {
    @Override
    public TargetModel target() {
      return TargetModel.JOIN_REQUEST;
    }
  }

  record JoinRequestApproved(UUID id) implements ModelUpdatedEvents {
    @Override
    public TargetModel target() {
      return TargetModel.JOIN_REQUEST;
    }
  }

  record JoinRequestWaitlisted(UUID id) implements ModelUpdatedEvents {
    @Override
    public TargetModel target() {
      return TargetModel.JOIN_REQUEST;
    }
  }

  record MeetupCreated(UUID id) implements ModelUpdatedEvents {
    @Override
    public TargetModel target() {
      return TargetModel.MEETUP;
    }
  }

  record MeetupRescheduled(UUID id) implements ModelUpdatedEvents {
    @Override
    public TargetModel target() {
      return TargetModel.MEETUP;
    }
  }

  record MeetupCanceled(UUID id) implements ModelUpdatedEvents {
    @Override
    public TargetModel target() {
      return TargetModel.MEETUP;
    }
  }
}

package org.bytewright.bgmo.adapter.api.frontend.view.meetup.model;

import com.vaadin.flow.component.html.Span;
import java.util.List;
import java.util.UUID;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;

public record MeetupAttendeesContext(
    UUID meetupId, boolean isMeetupCanceled, List<AttendeeRequestItem> requests) {

  public record AttendeeRequestItem(MeetupJoinRequest request, Span resolvedContact) {}
}

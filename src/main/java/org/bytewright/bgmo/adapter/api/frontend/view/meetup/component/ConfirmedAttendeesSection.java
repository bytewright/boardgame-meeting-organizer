package org.bytewright.bgmo.adapter.api.frontend.view.meetup.component;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.List;

/**
 * Public section showing the display names of confirmed attendees.
 *
 * <p>Contact info is deliberately never included here — it is shown only in the organiser's
 * attendee management view.
 */
public class ConfirmedAttendeesSection extends VerticalLayout {

  public ConfirmedAttendeesSection(boolean isOrganizer, List<String> names) {
    setPadding(false);
    setSpacing(true);

    add(new H3(getTranslation("meetup.confirmedAttendees")));

    if (names.isEmpty()) {
      if (!isOrganizer) {
        add(new Span(getTranslation("meetup.confirmedAttendeesNone")));
      } else {
        add(new Span(getTranslation("meetup.confirmedAttendeesNoneOwner")));
      }
      return;
    }

    VerticalLayout nameList = new VerticalLayout();
    nameList.setPadding(false);
    nameList.setSpacing(false);
    names.stream().map(name -> new Span("• " + name)).forEach(nameList::add);
    add(nameList);
  }
}

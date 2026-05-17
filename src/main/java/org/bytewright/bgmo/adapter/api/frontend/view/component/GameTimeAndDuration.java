package org.bytewright.bgmo.adapter.api.frontend.view.component;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.dom.Style;

public class GameTimeAndDuration extends HorizontalLayout {

  public GameTimeAndDuration(String timeStr, int durationHours) {
    Icon clockIcon = VaadinIcon.CLOCK.create();
    clockIcon.setSize("var(--lumo-icon-size-s)");
    clockIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
    Span timeSpan = new Span(timeStr + " " + getTranslation("meetup.time.suffix"));

    Icon timerIcon = VaadinIcon.TIMER.create();
    timerIcon.setSize("var(--lumo-icon-size-s)");
    timerIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
    Span durationSpan = new Span(getTranslation("meetup.duration", durationHours));

    // Each icon+label pair is its own flex item, so they wrap together as a unit
    HorizontalLayout timeGroup = new HorizontalLayout(clockIcon, timeSpan);
    timeGroup.setSpacing(true);
    timeGroup.setAlignItems(Alignment.CENTER);
    timeGroup.getStyle().set("flex", "1 1 0").set("min-width", "140px");

    HorizontalLayout durationGroup = new HorizontalLayout(timerIcon, durationSpan);
    durationGroup.setSpacing(true);
    durationGroup.setAlignItems(Alignment.CENTER);
    durationGroup.getStyle().set("flex", "1 1 0").set("min-width", "140px");

    add(timeGroup, durationGroup);
    setSpacing(true);
    setAlignItems(Alignment.CENTER);
    // Pairs wrap to a new line on narrow screens
    getStyle().setFlexWrap(Style.FlexWrap.WRAP);
  }
}

package org.bytewright.bgmo.adapter.api.frontend.view.component;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class GameTimeAndDuration extends HorizontalLayout {
  public GameTimeAndDuration(String timeStr, int durationHours) {
    Span timeSpan = new Span(timeStr + " " + getTranslation("meetup.time.suffix"));
    timeSpan.setMinWidth(200, Unit.PIXELS);
    Span durationSpan = new Span(getTranslation("meetup.duration", durationHours));

    Icon clockIcon = VaadinIcon.CLOCK.create();
    clockIcon.setSize("var(--lumo-icon-size-s)");
    clockIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");

    Icon timerIcon = VaadinIcon.TIMER.create();
    timerIcon.setSize("var(--lumo-icon-size-s)");
    timerIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
    add(clockIcon, timeSpan, timerIcon, durationSpan);
    setSpacing(true);
    setAlignItems(Alignment.CENTER);
  }
}

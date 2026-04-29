package org.bytewright.bgmo.adapter.api.frontend.view.component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/** A clickable card that navigates to a sub-page. */
public class NavCardComponent extends Div {
  /**
   * @param icon icon to display prominently
   * @param title card heading
   * @param subtitle secondary description or status line
   * @param highlighted whether to draw attention (e.g. pending items exist)
   * @param onClick navigation action
   */
  public NavCardComponent(
      VaadinIcon icon, String title, String subtitle, boolean highlighted, Runnable onClick) {
    Icon ic = icon.create();
    ic.setSize("2em");
    ic.getStyle()
        .set("color", highlighted ? "var(--lumo-error-color)" : "var(--lumo-primary-color)");

    H3 heading = new H3(title);
    heading.getStyle().set("margin", "0");

    Paragraph desc = new Paragraph(subtitle);
    desc.getStyle()
        .set("margin", "0")
        .set("font-size", "var(--lumo-font-size-s)")
        .set("color", highlighted ? "var(--lumo-error-color)" : "var(--lumo-secondary-text-color)");

    VerticalLayout text = new VerticalLayout(heading, desc);
    text.setPadding(false);
    text.setSpacing(false);

    Div card = new Div(ic, text);
    card.getStyle()
        .set("display", "flex")
        .set("flex-direction", "column")
        .set("gap", "var(--lumo-space-m)")
        .set("padding", "var(--lumo-space-l)")
        .set(
            "border",
            "2px solid " + (highlighted ? "var(--lumo-error-color)" : "var(--lumo-contrast-20pct)"))
        .set("border-radius", "var(--lumo-border-radius-l)")
        .set("cursor", "pointer")
        .set("flex", "1")
        .set("transition", "background-color 0.15s ease");

    card.getElement()
        .addEventListener(
            "mouseover", e -> card.getStyle().set("background-color", "var(--lumo-contrast-5pct)"));
    card.getElement().addEventListener("mouseout", e -> card.getStyle().remove("background-color"));
    card.addClickListener(e -> onClick.run());
  }
}

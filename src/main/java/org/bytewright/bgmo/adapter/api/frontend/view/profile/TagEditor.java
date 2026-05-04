package org.bytewright.bgmo.adapter.api.frontend.view.profile;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/**
 * Chip-style editor for a list of short string tags.
 *
 * <p>Existing tags are shown as removable badge-style chips. A text field with an "Add" button
 * (also triggered by Enter) appends new tags. Duplicate tags are silently ignored.
 */
public class TagEditor extends VerticalLayout {

  @Getter private final List<String> tags;
  private final FlexLayout chipsLayout;

  public TagEditor(List<String> initialTags) {
    this.tags = initialTags != null ? new ArrayList<>(initialTags) : new ArrayList<>();

    setPadding(false);
    setSpacing(true);
    setWidthFull();

    Span label = new Span(getTranslation("gamelib.field.tags"));
    label
        .getStyle()
        .set("font-size", "var(--lumo-font-size-s)")
        .set("color", "var(--lumo-secondary-text-color)");

    chipsLayout = new FlexLayout();
    chipsLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
    chipsLayout.getStyle().set("gap", "var(--lumo-space-xs)");
    chipsLayout.setWidthFull();

    TextField newTagField = new TextField();
    newTagField.setPlaceholder(getTranslation("gamelib.field.tag.placeholder"));
    newTagField.setWidthFull();

    Button addBtn = new Button(VaadinIcon.PLUS.create());
    addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Runnable addTag =
        () -> {
          String val = newTagField.getValue().trim();
          if (!val.isBlank() && !tags.contains(val)) {
            tags.add(val);
            refreshChips();
            newTagField.clear();
          }
        };

    addBtn.addClickListener(e -> addTag.run());
    newTagField.addKeyPressListener(Key.ENTER, e -> addTag.run());

    HorizontalLayout addRow = new HorizontalLayout(newTagField, addBtn);
    addRow.setAlignItems(FlexComponent.Alignment.BASELINE);
    addRow.setWidthFull();
    addRow.expand(newTagField);
    addRow.setPadding(false);

    add(label, chipsLayout, addRow);
    refreshChips();
  }

  private void refreshChips() {
    chipsLayout.removeAll();
    for (String tag : tags) {
      chipsLayout.add(buildChip(tag));
    }
  }

  private HorizontalLayout buildChip(String tag) {
    Span tagLabel = new Span(tag);
    tagLabel.getStyle().set("font-size", "var(--lumo-font-size-s)");

    Button removeBtn =
        new Button(
            VaadinIcon.CLOSE_SMALL.create(),
            e -> {
              tags.remove(tag);
              refreshChips();
            });
    removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    removeBtn
        .getStyle()
        .set("margin", "0")
        .set("padding", "0 2px")
        .set("min-width", "unset")
        .set("height", "unset");

    HorizontalLayout chip = new HorizontalLayout(tagLabel, removeBtn);
    chip.setAlignItems(FlexComponent.Alignment.CENTER);
    chip.setSpacing(false);
    chip.setPadding(false);
    chip.getStyle()
        .set("background", "var(--lumo-primary-color-10pct)")
        .set("border-radius", "var(--lumo-border-radius-l)")
        .set("padding", "4px 8px")
        .set("gap", "4px")
        .set("cursor", "default");
    return chip;
  }
}

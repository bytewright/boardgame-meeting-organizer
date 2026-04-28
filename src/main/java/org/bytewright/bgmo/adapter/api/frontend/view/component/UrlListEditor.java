package org.bytewright.bgmo.adapter.api.frontend.view.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.Getter;

public class UrlListEditor extends VerticalLayout {

  @Getter private final List<String> urls;
  private final Consumer<String> newValueConsumer;
  private final Consumer<String> deletionConsumer;
  private final VerticalLayout rowsContainer;

  public UrlListEditor(
      List<String> stringList,
      Consumer<String> newValueConsumer,
      Consumer<String> deletionConsumer) {
    this.urls = stringList != null ? new ArrayList<>(stringList) : new ArrayList<>();
    this.newValueConsumer = newValueConsumer;
    this.deletionConsumer = deletionConsumer;

    setPadding(false);
    setSpacing(true);
    setWidthFull();

    rowsContainer = new VerticalLayout();
    rowsContainer.setPadding(false);
    rowsContainer.setSpacing(false);
    rowsContainer.setWidthFull();

    add(rowsContainer, createAddButton());

    refreshRows();
  }

  private void refreshRows() {
    rowsContainer.removeAll();
    for (String url : urls) {
      rowsContainer.add(createRow(url));
    }
  }

  private HorizontalLayout createRow(String initialValue) {
    HorizontalLayout row = new HorizontalLayout();
    row.setWidthFull();
    row.setAlignItems(Alignment.CENTER);

    TextField urlField = new TextField();
    urlField.setValue(initialValue != null ? initialValue : "");
    urlField.setWidthFull();

    Button saveBtn = new Button(VaadinIcon.CHECK.create());
    saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveBtn.setEnabled(false);

    Button deleteBtn = new Button(VaadinIcon.TRASH.create());
    deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

    // Enable save when value changes
    urlField.addValueChangeListener(e -> saveBtn.setEnabled(true));

    saveBtn.addClickListener(
        e -> {
          String newValue = urlField.getValue();
          // update list
          urls.remove(initialValue);
          urls.add(newValue);
          newValueConsumer.accept(newValue);
          saveBtn.setEnabled(false);
        });

    deleteBtn.addClickListener(
        e -> {
          String valueToDelete = urlField.getValue();
          deletionConsumer.accept(valueToDelete);
          urls.remove(valueToDelete);
          refreshRows();
        });

    row.add(urlField, saveBtn, deleteBtn);
    return row;
  }

  private Button createAddButton() {
    Button addBtn = new Button("Add URL", VaadinIcon.PLUS.create());
    addBtn.setWidthFull();
    addBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    addBtn.addClickListener(
        e -> {
          String newUrl = "http://google.com/";
          newValueConsumer.accept(newUrl);
          urls.add(newUrl);
          refreshRows();
        });

    return addBtn;
  }
}

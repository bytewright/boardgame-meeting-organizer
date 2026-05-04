package org.bytewright.bgmo.adapter.api.frontend.view.profile;

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
import org.bytewright.bgmo.domain.model.Game;

/**
 * Editor for a list of {@link Game.UserLink} entries.
 *
 * <p>Each row exposes two fields: the target URL and an optional display text. A mutable holder
 * array ({@code current[]}) ensures save/delete lambdas always reference the committed version of
 * the link, even after in-row edits.
 */
public class UrlListEditor extends VerticalLayout {

  @Getter private final List<Game.UserLink> urls;
  private final Consumer<Game.UserLink> newValueConsumer;
  private final Consumer<Game.UserLink> deletionConsumer;
  private final VerticalLayout rowsContainer;

  public UrlListEditor(
      List<Game.UserLink> linkList,
      Consumer<Game.UserLink> newValueConsumer,
      Consumer<Game.UserLink> deletionConsumer) {
    this.urls = linkList != null ? new ArrayList<>(linkList) : new ArrayList<>();
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
    for (Game.UserLink link : urls) {
      rowsContainer.add(createRow(link));
    }
  }

  private HorizontalLayout createRow(Game.UserLink initialLink) {
    // Mutable holder so save/delete lambdas always see the committed version of the link.
    Game.UserLink[] current = {initialLink};

    HorizontalLayout row = new HorizontalLayout();
    row.setWidthFull();
    row.setAlignItems(Alignment.CENTER);

    TextField urlField = new TextField(getTranslation("gamelib.field.url"));
    urlField.setPlaceholder(getTranslation("gamelib.field.url.placeholder"));
    urlField.setValue(initialLink.url() != null ? initialLink.url() : "");
    urlField.setWidthFull();

    TextField displayTextField = new TextField(getTranslation("gamelib.field.url.displayText"));
    displayTextField.setPlaceholder(getTranslation("gamelib.field.url.displayText.placeholder"));
    displayTextField.setValue(initialLink.displayText() != null ? initialLink.displayText() : "");
    displayTextField.setWidth("40%");

    Button saveBtn = new Button(VaadinIcon.CHECK.create());
    saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveBtn.setEnabled(false);

    Button deleteBtn = new Button(VaadinIcon.TRASH.create());
    deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

    urlField.addValueChangeListener(e -> saveBtn.setEnabled(true));
    displayTextField.addValueChangeListener(e -> saveBtn.setEnabled(true));

    saveBtn.addClickListener(
        e -> {
          Game.UserLink updated =
              new Game.UserLink(urlField.getValue(), displayTextField.getValue());
          urls.remove(current[0]);
          deletionConsumer.accept(current[0]);
          urls.add(updated);
          newValueConsumer.accept(updated);
          current[0] = updated;
          saveBtn.setEnabled(false);
        });

    deleteBtn.addClickListener(
        e -> {
          urls.remove(current[0]);
          deletionConsumer.accept(current[0]);
          refreshRows();
        });

    row.add(urlField, displayTextField, saveBtn, deleteBtn);
    row.expand(urlField);
    return row;
  }

  private Button createAddButton() {
    Button addBtn = new Button(getTranslation("gamelib.action.add_url"), VaadinIcon.PLUS.create());
    addBtn.setWidthFull();
    addBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    addBtn.addClickListener(
        e -> {
          Game.UserLink blank = new Game.UserLink("", "");
          urls.add(blank);
          newValueConsumer.accept(blank);
          refreshRows();
        });

    return addBtn;
  }
}

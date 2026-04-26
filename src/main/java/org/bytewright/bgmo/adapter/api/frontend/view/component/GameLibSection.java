package org.bytewright.bgmo.adapter.api.frontend.view.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import lombok.Setter;
import org.bytewright.bgmo.domain.model.Game;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.GameDao;
import org.bytewright.bgmo.usecases.UserWorkflows;

public class GameLibSection extends VerticalLayout {
  private final UserWorkflows userWorkflows;
  private final GameDao gameDao;
  private final VerticalLayout listContainer;
  @Setter private RegisteredUser currentUser;

  public GameLibSection(UserWorkflows userWorkflows, GameDao gameDao, RegisteredUser currentUser) {
    this.userWorkflows = userWorkflows;
    this.gameDao = gameDao;
    this.currentUser = currentUser;
    // Mobile-first centering and padding
    setAlignItems(Alignment.CENTER);
    setPadding(true);
    setSpacing(true);
    getStyle().set("max-width", MainLayout.MAX_DISPLAYPORT_WIDTH).set("margin", "0 auto");

    H2 title = new H2(getTranslation("gamelib.title"));

    Button addGameBtn =
        new Button(
            getTranslation("gamelib.add-new"), VaadinIcon.PLUS.create(), e -> addNewGameRow());
    addGameBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
    addGameBtn.setWidthFull();

    listContainer = new VerticalLayout();
    listContainer.setPadding(false);
    listContainer.setWidthFull();

    add(title, addGameBtn, listContainer);
    refreshLibrary();
  }

  private void createGameRow(Game game) {
    createGameRow(game, false);
  }

  private void createGameRow(Game game, boolean rowOpened) {
    // --- Summary (The "Row" when collapsed) ---
    HorizontalLayout summary = new HorizontalLayout();
    summary.setAlignItems(Alignment.CENTER);
    summary.setWidthFull();

    Image avatar =
        new Image(
            game.getArtworkLink() != null ? game.getArtworkLink() : "images/default-game.png",
            "Game art");
    avatar.setWidth("50px");
    avatar.setHeight("50px");
    avatar.getStyle().set("border-radius", "4px").set("object-fit", "cover");

    Span name = new Span(game.getName());
    name.getStyle().set("font-weight", "bold").set("flex-grow", "1");

    summary.add(avatar, name);

    // --- Content (The "Editor" when expanded) ---
    VerticalLayout editorLayout = new VerticalLayout();
    editorLayout.setPadding(true);
    editorLayout.setSpacing(true);

    TextField nameField = new TextField(getTranslation("gamelib.field.name"));
    nameField.setValue(game.getName() != null ? game.getName() : "");
    nameField.setWidthFull();

    TextField artworkField = new TextField(getTranslation("gamelib.field.artwork"));
    artworkField.setValue(game.getArtworkLink() != null ? game.getArtworkLink() : "");
    artworkField.setWidthFull();

    TextArea descField = new TextArea(getTranslation("gamelib.field.description"));
    descField.setValue(game.getDescription() != null ? game.getDescription() : "");
    descField.setWidthFull();

    HorizontalLayout playersRow = new HorizontalLayout();
    IntegerField minP = new IntegerField(getTranslation("gamelib.field.minPlayers"));
    minP.setValue(game.getMinPlayers());
    IntegerField maxP = new IntegerField(getTranslation("gamelib.field.maxPlayers"));
    maxP.setValue(game.getMaxPlayers());
    playersRow.add(minP, maxP);

    HorizontalLayout actions = new HorizontalLayout();
    Button saveBtn =
        new Button(
            getTranslation("gamelib.action.save"),
            e -> {
              game.setName(nameField.getValue());
              game.setArtworkLink(artworkField.getValue());
              game.setDescription(descField.getValue());
              game.setMinPlayers(minP.getValue() != null ? minP.getValue() : 1);
              game.setMaxPlayers(maxP.getValue() != null ? maxP.getValue() : 1);
              game.setOwnerId(currentUser.getId());
              userWorkflows.addGameToLibrary(currentUser.getId(), game);
              Notification.show(getTranslation("gamelib.notif.saved"));
              refreshLibrary();
            });
    saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button deleteBtn =
        new Button(
            getTranslation("gamelib.action.delete"),
            e -> {
              // TODO: Add confirmation dialog to really delete
              if (game.getId() != null) {
                userWorkflows.removeGameFromLibrary(game.getId());
              }
              refreshLibrary();
            });
    deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

    actions.add(saveBtn, deleteBtn);
    editorLayout.add(nameField, artworkField, descField, playersRow, actions);

    Details row = new Details(summary, editorLayout);
    row.setOpened(rowOpened);
    row.setWidthFull();
    row.getStyle()
        .set("background", "var(--lumo-base-color)")
        .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

    listContainer.add(row);
  }

  private void addNewGameRow() {
    // TODO: This should become a Dialog, on click a user should
    Game newGame = Game.builder().minPlayers(1).maxPlayers(4).build();
    createGameRow(newGame, true);
  }

  public void refreshLibrary() {
    listContainer.removeAll();
    gameDao.findByOwnerId(currentUser.getId()).forEach(this::createGameRow);
  }
}

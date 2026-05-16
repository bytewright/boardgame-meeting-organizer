package org.bytewright.bgmo.adapter.api.frontend.view.profile;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.adapter.api.frontend.view.component.factory.ComponentFactory;
import org.bytewright.bgmo.domain.model.Game;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.GameDao;
import org.bytewright.bgmo.usecases.UserWorkflows;

public class GameLibSection extends VerticalLayout {
  private final ComponentFactory componentFactory;
  private final UserWorkflows userWorkflows;
  private final GameDao gameDao;
  private final VerticalLayout listContainer;
  private final RegisteredUser currentUser;

  public GameLibSection(
      ComponentFactory componentFactory,
      UserWorkflows userWorkflows,
      GameDao gameDao,
      RegisteredUser currentUser) {
    this.componentFactory = componentFactory;
    this.userWorkflows = userWorkflows;
    this.gameDao = gameDao;
    this.currentUser = currentUser;
    setAlignItems(Alignment.CENTER);
    setPadding(true);
    setSpacing(true);
    setMaxWidth(MainLayout.MAX_DISPLAYPORT_WIDTH);
    getStyle().set("margin", "0 auto");

    Button addGameBtn =
        new Button(
            getTranslation("gamelib.add-new"),
            VaadinIcon.PLUS.create(),
            this::createAddNewGameDialog);
    addGameBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
    addGameBtn.setWidthFull();

    listContainer = new VerticalLayout();
    listContainer.setPadding(false);
    listContainer.setWidthFull();

    add(addGameBtn, listContainer);
    refreshLibrary();
  }

  private void createGameRow(Game game) {
    createGameRow(game, false);
  }

  private void createGameRow(Game game, boolean rowOpened) {
    // ToDo this whole method should use Binders to the game model!

    // --- Summary (the "Row" when collapsed) ---
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

    // --- Content (the "Editor" when expanded) ---
    VerticalLayout editorLayout = new VerticalLayout();
    editorLayout.setPadding(true);
    editorLayout.setSpacing(true);

    // -- Basic info --
    TextField nameField = new TextField(getTranslation("gamelib.field.name"));
    nameField.setValue(game.getName() != null ? game.getName() : "");
    nameField.setWidthFull();

    TextField artworkField = new TextField(getTranslation("gamelib.field.artwork"));
    artworkField.setValue(game.getArtworkLink() != null ? game.getArtworkLink() : "");
    artworkField.setWidthFull();

    TextArea descField = new TextArea(getTranslation("gamelib.field.description"));
    descField.setValue(game.getDescription() != null ? game.getDescription() : "");
    descField.setWidthFull();

    // -- Notes --
    TextArea notesField = new TextArea(getTranslation("gamelib.field.notes"));
    notesField.setValue(game.getNotes() != null ? game.getNotes() : "");
    notesField.setHelperText(getTranslation("gamelib.field.notes.helper"));
    notesField.setWidthFull();

    // -- Player counts --
    IntegerField minP = new IntegerField(getTranslation("gamelib.field.minPlayers"));
    minP.setValue(game.getMinPlayers());
    minP.setMaxWidth(125, Unit.PIXELS);
    IntegerField maxP = new IntegerField(getTranslation("gamelib.field.maxPlayers"));
    maxP.setValue(game.getMaxPlayers());
    maxP.setMaxWidth(125, Unit.PIXELS);
    IntegerField optimalPlayerCount =
        new IntegerField(getTranslation("gamelib.field.optimalPlayers"));
    optimalPlayerCount.setValue(game.getOptimalPlayers());
    optimalPlayerCount.setMaxWidth(125, Unit.PIXELS);
    HorizontalLayout playersRow = new HorizontalLayout(minP, maxP, optimalPlayerCount);

    // -- BGG / meta --
    IntegerField bggId = new IntegerField(getTranslation("gamelib.field.bggId"));
    bggId.setValue(game.getBggId() != null ? Math.toIntExact(game.getBggId()) : null);
    bggId.setWidthFull();

    IntegerField playTimePerPlayer =
        new IntegerField(getTranslation("gamelib.field.playTimePerPlayer"));
    playTimePerPlayer.setValue(game.getPlayTimeMinutesPerPlayer());
    playTimePerPlayer.setWidthFull();

    NumberField complexity = new NumberField(getTranslation("gamelib.field.complexity"));
    complexity.setStep(0.001);
    complexity.setMin(1);
    complexity.setMax(5);
    complexity.setValue(game.getComplexity());
    complexity.setWidthFull();

    // -- Tags --
    // TagEditor owns a mutable copy; we read it back on save.
    TagEditor tagEditor = new TagEditor(game.getTags());
    tagEditor.setWidthFull();

    // -- URLs --
    // Consumers update the game's list in-place, consistent with the existing save pattern.
    UrlListEditor urlListEditor =
        new UrlListEditor(game.getUrls(), newLink -> {}, forDeletion -> {});
    urlListEditor.setWidthFull();

    // -- Actions --
    Button saveBtn =
        new Button(
            getTranslation("gamelib.action.save"),
            e ->
                handleSaveEvent(
                    game,
                    nameField,
                    artworkField,
                    descField,
                    notesField,
                    minP,
                    maxP,
                    optimalPlayerCount,
                    bggId,
                    complexity,
                    playTimePerPlayer,
                    tagEditor.getTags(),
                    urlListEditor.getUrls()));
    saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button deleteBtn =
        new Button(
            getTranslation("gamelib.action.delete"),
            e -> {
              if (game.getId() != null) {
                ConfirmDialog confirm = new ConfirmDialog();
                confirm.setHeader(getTranslation("gamelib.confirm.delete.title"));
                confirm.setText(getTranslation("gamelib.confirm.delete.body", game.getName()));
                confirm.setCancelable(true);
                confirm.addConfirmListener(
                    evt -> {
                      userWorkflows.removeGameFromLibrary(game.getId());
                      refreshLibrary();
                    });
                confirm.open();
              }
            });
    deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

    HorizontalLayout actions = new HorizontalLayout(saveBtn, deleteBtn);

    editorLayout.add(
        nameField,
        artworkField,
        descField,
        notesField,
        playersRow,
        bggId,
        playTimePerPlayer,
        complexity,
        tagEditor,
        urlListEditor,
        actions);

    Details row = new Details(summary, editorLayout);
    row.setOpened(rowOpened);
    row.setWidthFull();
    row.getStyle()
        .set("background", "var(--lumo-base-color)")
        .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

    listContainer.add(row);
  }

  private void handleSaveEvent(
      Game game,
      TextField nameField,
      TextField artworkField,
      TextArea descField,
      TextArea notesField,
      IntegerField minP,
      IntegerField maxP,
      IntegerField optimalP,
      IntegerField bggIdField,
      NumberField complexityField,
      IntegerField playTimeField,
      List<String> tagList,
      List<Game.UserLink> urlList) {
    game.setName(nameField.getValue());
    game.setArtworkLink(artworkField.getValue().isBlank() ? null : artworkField.getValue());
    game.setDescription(descField.getValue().isBlank() ? null : descField.getValue());
    game.setNotes(notesField.getValue().isBlank() ? null : notesField.getValue());
    game.setMinPlayers(minP.getValue() != null ? minP.getValue() : 1);
    game.setMaxPlayers(maxP.getValue() != null ? maxP.getValue() : 1);
    game.setOptimalPlayers(optimalP.getValue());
    game.setBggId(bggIdField.getValue() != null ? bggIdField.getValue().longValue() : null);
    game.setPlayTimeMinutesPerPlayer(playTimeField.getValue());
    game.setComplexity(complexityField.getValue());
    // Replace the list contents
    game.getTags().clear();
    game.getTags().addAll(tagList);
    game.getUrls().clear();
    game.getUrls().addAll(urlList);
    game.setOwnerId(currentUser.getId());
    userWorkflows.updateGameInLibrary(currentUser.getId(), game);
    Notification.show(getTranslation("gamelib.notif.saved"));
    refreshLibrary();
  }

  private void createAddNewGameDialog(ClickEvent<Button> ignore) {
    AddGameDialog addGameDialog = componentFactory.addGameDialog(currentUser, this::refreshLibrary);
    addGameDialog.open();
  }

  public void refreshLibrary() {
    listContainer.removeAll();
    gameDao.findByOwnerId(currentUser.getId()).forEach(this::createGameRow);
  }
}

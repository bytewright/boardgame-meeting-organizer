package org.bytewright.bgmo.adapter.api.frontend.view.profile;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.bytewright.bgmo.domain.model.Game;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.GameInformationProvider;

public class AddGameDialog extends Dialog {
  private static final int DIALOG_WIDTH_PX = 600;
  private final List<GameInformationProvider> providers;
  private final BiConsumer<RegisteredUser, Game.Creation> saveConsumer;
  private final RegisteredUser currentUser;
  private final VerticalLayout phaseContainer;

  public AddGameDialog(
      List<GameInformationProvider> providers,
      RegisteredUser currentUser,
      BiConsumer<RegisteredUser, Game.Creation> saveConsumer) {
    this.providers = providers;
    this.currentUser = currentUser;
    this.saveConsumer = saveConsumer;

    setHeaderTitle(getTranslation("gamelib.add.dialog.title"));
    setWidth(DIALOG_WIDTH_PX, Unit.PIXELS);
    setCloseOnEsc(true);
    setCloseOnOutsideClick(false);

    phaseContainer = new VerticalLayout();
    phaseContainer.setPadding(false);
    phaseContainer.setSpacing(true);
    add(phaseContainer);

    showPhase1();
  }

  // ---------------------------------------------------------------------------
  // Phase 1: Source selection
  // ---------------------------------------------------------------------------

  private void showPhase1() {
    phaseContainer.removeAll();
    getFooter().removeAll();

    Locale locale = getLocale();

    java.util.List<SourceOption> options = new java.util.ArrayList<>();
    options.add(SourceOption.manual(getTranslation("gamelib.add.source.manual")));
    providers.stream().map(p -> SourceOption.fromProvider(p, locale)).forEach(options::add);

    ComboBox<SourceOption> sourceCombo = new ComboBox<>(getTranslation("gamelib.add.source.label"));
    sourceCombo.setItems(options);
    sourceCombo.setItemLabelGenerator(SourceOption::displayName);
    sourceCombo.setValue(options.getFirst());
    sourceCombo.setWidthFull();
    sourceCombo.setAllowCustomValue(false);

    Image providerLogo = new Image();
    providerLogo.setHeight("32px");
    providerLogo.setVisible(false);

    TextField providerInputField = new TextField();
    providerInputField.setWidthFull();
    providerInputField.setVisible(false);

    sourceCombo.addValueChangeListener(
        e -> {
          SourceOption selected = e.getValue();
          if (selected == null || selected.isManual()) {
            providerLogo.setVisible(false);
            providerInputField.setVisible(false);
          } else {
            if (selected.logoLink() != null) {
              providerLogo.setSrc(selected.logoLink());
              providerLogo.setVisible(true);
            } else {
              providerLogo.setVisible(false);
            }
            providerInputField.setLabel(selected.inputHint());
            providerInputField.setHelperText(selected.inputHint());
            providerInputField.setVisible(true);
            providerInputField.setInvalid(false);
          }
        });

    phaseContainer.add(sourceCombo, providerLogo, providerInputField);

    Button cancelBtn = new Button(getTranslation("action.cancel"), e -> close());
    cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    Button nextBtn =
        new Button(
            getTranslation("action.next"),
            VaadinIcon.ARROW_RIGHT.create(),
            e -> handlePhase1Next(sourceCombo.getValue(), providerInputField));
    nextBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    nextBtn.setIconAfterText(true);

    getFooter().add(cancelBtn, nextBtn);
  }

  private void handlePhase1Next(SourceOption selected, TextField providerInputField) {
    if (selected == null) return;

    if (selected.isManual()) {
      showPhase2(Game.Creation.builder().minPlayers(1).maxPlayers(4).build());
      return;
    }

    String input = providerInputField.getValue();

    Function<String, Boolean> validator = selected.inputValidator();
    if (validator != null && !validator.apply(input)) {
      providerInputField.setInvalid(true);
      providerInputField.setErrorMessage(getTranslation("gamelib.add.source.input.invalid"));
      return;
    }
    providerInputField.setInvalid(false);

    Optional<Game.Creation> result = selected.provider().generateGame(input);
    if (result.isEmpty()) {
      Notification n = Notification.show(getTranslation("gamelib.add.source.lookup.failed"));
      n.addThemeVariants(NotificationVariant.LUMO_ERROR);
      return;
    }

    showPhase2(result.get());
  }

  // ---------------------------------------------------------------------------
  // Phase 2: Game editor (pre-populated from provider or blank for manual)
  // ---------------------------------------------------------------------------

  private void showPhase2(Game.Creation prefill) {
    phaseContainer.removeAll();
    getFooter().removeAll();

    // -- Basic info --
    TextField nameField = new TextField(getTranslation("gamelib.field.name"));
    nameField.setValue(prefill.getName() != null ? prefill.getName() : "");
    nameField.setRequired(true);
    nameField.setWidthFull();

    TextField artworkField = new TextField(getTranslation("gamelib.field.artwork"));
    artworkField.setValue(prefill.getArtworkLink() != null ? prefill.getArtworkLink() : "");
    artworkField.setWidthFull();

    TextArea descField = new TextArea(getTranslation("gamelib.field.description"));
    descField.setValue(prefill.getDescription() != null ? prefill.getDescription() : "");
    descField.setWidthFull();

    // -- Notes (owner-specific, not shown publicly in description) --
    // i18n key: gamelib.field.notes
    TextArea notesField = new TextArea(getTranslation("gamelib.field.notes"));
    notesField.setValue(prefill.getNotes() != null ? prefill.getNotes() : "");
    notesField.setWidthFull();
    notesField.setHelperText(getTranslation("gamelib.field.notes.helper"));

    // -- Player counts --
    IntegerField minP = new IntegerField(getTranslation("gamelib.field.minPlayers"));
    minP.setValue(prefill.getMinPlayers() > 0 ? prefill.getMinPlayers() : 1);
    minP.setMin(1);
    minP.setMaxWidth(160, Unit.PIXELS);
    IntegerField maxP = new IntegerField(getTranslation("gamelib.field.maxPlayers"));
    maxP.setValue(prefill.getMaxPlayers() > 0 ? prefill.getMaxPlayers() : 4);
    maxP.setMin(1);
    maxP.setMaxWidth(160, Unit.PIXELS);
    IntegerField optimalP = new IntegerField(getTranslation("gamelib.field.optimalPlayers"));
    optimalP.setValue(prefill.getOptimalPlayers() != null ? prefill.getOptimalPlayers() : 3);
    optimalP.setMin(1);
    optimalP.setMaxWidth(160, Unit.PIXELS);
    HorizontalLayout playersRow = new HorizontalLayout(minP, maxP, optimalP);

    // -- BGG / meta --
    IntegerField bggIdField = new IntegerField(getTranslation("gamelib.field.bggId"));
    bggIdField.setWidthFull();
    if (prefill.getBggId() != null) bggIdField.setValue(prefill.getBggId().intValue());

    NumberField complexityField = new NumberField(getTranslation("gamelib.field.complexity"));
    complexityField.setMin(1.0);
    complexityField.setMax(5.0);
    complexityField.setStep(0.001);
    complexityField.setWidth((float) (Math.floor(DIALOG_WIDTH_PX / 2.) - 15), Unit.PIXELS);
    if (prefill.getComplexity() != null) complexityField.setValue(prefill.getComplexity());

    IntegerField playTimeField =
        new IntegerField(getTranslation("gamelib.field.playTimePerPlayer"));
    playTimeField.setSuffixComponent(new Span(getTranslation("gamelib.field.playTime.suffix")));
    if (prefill.getPlayTimeMinutesPerPlayer() != null)
      playTimeField.setValue(prefill.getPlayTimeMinutesPerPlayer());
    playTimeField.setWidth((float) (Math.floor(DIALOG_WIDTH_PX / 2.) - 15), Unit.PIXELS);

    HorizontalLayout metaRow = new HorizontalLayout(complexityField, playTimeField);
    metaRow.setWidthFull();

    // -- Tags --
    TagEditor tagEditor = new TagEditor(prefill.getTags());

    // -- URL list -- (consumers are no-ops here; handleSave reads urlListEditor.getUrls() directly)
    UrlListEditor urlListEditor =
        new UrlListEditor(
            prefill.getUrls() != null ? prefill.getUrls() : List.of(), link -> {}, link -> {});

    phaseContainer.add(
        nameField,
        artworkField,
        descField,
        notesField,
        playersRow,
        metaRow,
        bggIdField,
        tagEditor,
        urlListEditor);

    // -- Footer --
    Button backBtn =
        new Button(
            getTranslation("action.back"), VaadinIcon.ARROW_LEFT.create(), e -> showPhase1());
    backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    Button saveBtn =
        new Button(
            getTranslation("gamelib.action.save"),
            e ->
                handleSave(
                    nameField,
                    artworkField,
                    descField,
                    notesField,
                    minP,
                    maxP,
                    optimalP,
                    bggIdField,
                    complexityField,
                    playTimeField,
                    tagEditor.getTags(),
                    urlListEditor.getUrls()));
    saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

    getFooter().add(backBtn, saveBtn);
  }

  private void handleSave(
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

    if (nameField.getValue().isBlank()) {
      nameField.setInvalid(true);
      nameField.setErrorMessage(getTranslation("gamelib.field.name.required"));
      return;
    }
    nameField.setInvalid(false);

    List<Game.UserLink> urls =
        urlList.stream().filter(l -> l.url() != null && !l.url().isBlank()).toList();

    Game.Creation creation =
        Game.Creation.builder()
            .name(nameField.getValue())
            .artworkLink(artworkField.getValue().isBlank() ? null : artworkField.getValue())
            .description(descField.getValue().isBlank() ? null : descField.getValue())
            .notes(notesField.getValue().isBlank() ? null : notesField.getValue())
            .minPlayers(minP.getValue() != null ? minP.getValue() : 1)
            .maxPlayers(maxP.getValue() != null ? maxP.getValue() : 1)
            .optimalPlayers(optimalP.getValue())
            .bggId(bggIdField.getValue() != null ? bggIdField.getValue().longValue() : null)
            .complexity(complexityField.getValue())
            .playTimeMinutesPerPlayer(playTimeField.getValue())
            .tags(tagList)
            .urls(urls)
            .build();
    saveConsumer.accept(currentUser, creation);
    Notification.show(getTranslation("gamelib.notif.saved"));
    close();
  }

  // ---------------------------------------------------------------------------
  // Source option wrapper
  // ---------------------------------------------------------------------------

  private record SourceOption(
      String displayName,
      String logoLink,
      String inputHint,
      Function<String, Boolean> inputValidator,
      GameInformationProvider provider) {

    static SourceOption manual(String label) {
      return new SourceOption(label, null, null, null, null);
    }

    static SourceOption fromProvider(GameInformationProvider provider, Locale locale) {
      GameInformationProvider.InputConfig config = provider.getInputConfig(locale);
      return new SourceOption(
          config.getProviderDisplayName(),
          config.getProviderLogoLink(),
          config.getInputHint(),
          config.getInputDataValidator(),
          provider);
    }

    boolean isManual() {
      return provider == null;
    }
  }
}

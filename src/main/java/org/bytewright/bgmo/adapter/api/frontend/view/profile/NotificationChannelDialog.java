package org.bytewright.bgmo.adapter.api.frontend.view.profile;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bytewright.bgmo.domain.model.notification.MessengerLinkContext;
import org.bytewright.bgmo.domain.model.notification.NotificationChannel;
import org.bytewright.bgmo.domain.model.notification.VerificationStep;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.usecases.NotificationWorkflows;

/**
 * Dialog for changing the user's notification channel.
 *
 * <p>Three modes via a radio group:
 *
 * <ul>
 *   <li><b>None</b> – disables notifications; confirmed via a Save button.
 *   <li><b>Email</b> – stores an email address independent of the user's contact options; confirmed
 *       via a Save button. Basic format validation only.
 *   <li><b>Telegram (already linked)</b> – shows linked status and an Unlink button which
 *       immediately sets the channel to None.
 *   <li><b>Telegram (not linked)</b> – embeds the bot-linking flow inline. The dialog closes
 *       automatically once the Refresh button confirms a successful link. Success is detected by
 *       checking {@code notificationChannel instanceof Telegram}, so users without a Telegram
 *       username (who get no ContactOption) are handled correctly.
 * </ul>
 *
 * <p>The {@code telegramLinkCtx} is built eagerly by {@code ComponentFactory} when the channel is
 * not already Telegram. The generated code sits in {@code pendingCodes} until it is consumed or the
 * app restarts; leaked codes are an accepted minor trade-off.
 */
public class NotificationChannelDialog extends Dialog {

  private static final String OPT_NONE = "none";
  private static final String OPT_EMAIL = "email";
  private static final String OPT_TELEGRAM = "telegram";

  private final UUID userId;
  private final NotificationChannel currentChannel;
  private final Optional<MessengerLinkContext> telegramLinkCtx;
  private final NotificationWorkflows notificationWorkflows;
  private final RegisteredUserDao userDao;
  private final Runnable onChanged;

  // Cleared and rebuilt each time the radio selection changes.
  private final VerticalLayout dynamicArea = new VerticalLayout();

  public NotificationChannelDialog(
      UUID userId,
      NotificationChannel currentChannel,
      Optional<MessengerLinkContext> telegramLinkCtx,
      NotificationWorkflows notificationWorkflows,
      RegisteredUserDao userDao,
      Runnable onChanged) {
    this.userId = userId;
    this.currentChannel = currentChannel;
    this.telegramLinkCtx = telegramLinkCtx;
    this.notificationWorkflows = notificationWorkflows;
    this.userDao = userDao;
    this.onChanged = onChanged;

    setHeaderTitle(getTranslation("profile.notifications.channel.dialog.title"));
    setWidth("440px");
    setCloseOnOutsideClick(true);

    dynamicArea.setPadding(false);
    dynamicArea.setSpacing(true);

    RadioButtonGroup<String> radioGroup = new RadioButtonGroup<>();
    radioGroup.setItems(OPT_NONE, OPT_EMAIL, OPT_TELEGRAM);
    radioGroup.setItemLabelGenerator(this::labelForOption);
    radioGroup.setValue(initialOption());
    radioGroup.addValueChangeListener(e -> renderDynamicArea(e.getValue()));

    renderDynamicArea(radioGroup.getValue());

    VerticalLayout content = new VerticalLayout(radioGroup, dynamicArea);
    content.setPadding(false);
    content.setSpacing(true);
    add(content);

    Button headerCloseBtn = new Button(VaadinIcon.CLOSE.create(), e -> close());
    headerCloseBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    getHeader().add(headerCloseBtn);

    Button cancelBtn = new Button(getTranslation("profile.contacts.action.cancel"), e -> close());
    cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    getFooter().add(cancelBtn);
  }

  // ---------------------------------------------------------------------------
  // Radio state
  // ---------------------------------------------------------------------------

  private String initialOption() {
    return switch (currentChannel) {
      case NotificationChannel.None ignored -> OPT_NONE;
      case NotificationChannel.Email ignored -> OPT_EMAIL;
      case NotificationChannel.Telegram ignored -> OPT_TELEGRAM;
    };
  }

  private String labelForOption(String option) {
    return switch (option) {
      case OPT_NONE -> getTranslation("profile.notifications.channel.none.label");
      case OPT_EMAIL -> getTranslation("profile.notifications.channel.email.label");
      case OPT_TELEGRAM -> getTranslation("profile.notifications.channel.telegram.label");
      default -> option;
    };
  }

  private void renderDynamicArea(String option) {
    dynamicArea.removeAll();
    switch (option) {
      case OPT_NONE -> renderNonePanel();
      case OPT_EMAIL -> renderEmailPanel();
      case OPT_TELEGRAM -> renderTelegramPanel();
    }
  }

  // ---------------------------------------------------------------------------
  // None panel
  // ---------------------------------------------------------------------------

  private void renderNonePanel() {
    Paragraph note = new Paragraph(getTranslation("profile.notifications.channel.none.explain"));
    note.getStyle()
        .set("margin", "0")
        .set("color", "var(--lumo-secondary-text-color)")
        .set("font-size", "var(--lumo-font-size-s)");

    Button saveBtn =
        new Button(getTranslation("profile.contacts.action.save"), VaadinIcon.CHECK.create());
    saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
    saveBtn.addClickListener(
        e -> {
          notificationWorkflows.linkNotificationChannel(userId, new NotificationChannel.None());
          close();
          onChanged.run();
        });

    dynamicArea.add(note, saveBtn);
  }

  // ---------------------------------------------------------------------------
  // Email panel
  // ---------------------------------------------------------------------------

  private void renderEmailPanel() {
    // Pre-fill if the current channel is already email.
    String currentEmail = currentChannel instanceof NotificationChannel.Email e ? e.email() : "";

    TextField emailField = new TextField(getTranslation("profile.contacts.email.title"));
    emailField.setWidthFull();
    emailField.setPlaceholder("name@example.com");
    emailField.setValue(currentEmail);
    emailField.setHelperText(getTranslation("profile.notifications.channel.email.helper"));

    Button saveBtn =
        new Button(getTranslation("profile.contacts.action.save"), VaadinIcon.CHECK.create());
    saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
    saveBtn.addClickListener(
        e -> {
          String value = emailField.getValue().trim();
          // Basic sanity guard — full validation is the user's responsibility.
          if (value.isBlank() || !value.contains("@")) {
            Notification.show(getTranslation("profile.contacts.email.invalid.notification"))
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
          }
          notificationWorkflows.linkNotificationChannel(
              userId, new NotificationChannel.Email(value));
          close();
          onChanged.run();
        });

    dynamicArea.add(emailField, saveBtn);
  }

  // ---------------------------------------------------------------------------
  // Telegram panel
  // ---------------------------------------------------------------------------

  private void renderTelegramPanel() {
    if (currentChannel instanceof NotificationChannel.Telegram) {
      renderTelegramLinked();
    } else {
      renderTelegramLinkingFlow();
    }
  }

  private void renderTelegramLinked() {
    Span status = new Span(getTranslation("profile.notifications.channel.telegram.linked"));
    status
        .getStyle()
        .set("color", "var(--lumo-success-color)")
        .set("font-size", "var(--lumo-font-size-s)");

    Button unlinkBtn =
        new Button(
            getTranslation("profile.notifications.channel.telegram.unlink"),
            VaadinIcon.UNLINK.create());
    unlinkBtn.addThemeVariants(
        ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    unlinkBtn.addClickListener(
        e -> {
          notificationWorkflows.linkNotificationChannel(userId, new NotificationChannel.None());
          close();
          onChanged.run();
        });

    HorizontalLayout row = new HorizontalLayout(status, unlinkBtn);
    row.setAlignItems(FlexComponent.Alignment.CENTER);
    row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
    row.setWidthFull();
    dynamicArea.add(row);
    // No save button: Unlink is the only action and it fires immediately.
  }

  private void renderTelegramLinkingFlow() {
    if (telegramLinkCtx.isEmpty()) {
      // Bot adapter not available (e.g. TELEGRAM_BOT_TOKEN not configured).
      Paragraph unavailable =
          new Paragraph(getTranslation("profile.notifications.channel.telegram.unavailable"));
      unavailable.getStyle().set("color", "var(--lumo-error-color)");
      dynamicArea.add(unavailable);
      return;
    }
    MessengerLinkContext ctx = telegramLinkCtx.get();
    dynamicArea.add(buildBotHandleRow(ctx));
    dynamicArea.add(buildCodeRow(ctx));
    buildStepsSection(ctx).ifPresent(dynamicArea::add);
    dynamicArea.add(buildStatusRow(ctx));
    // No save button: the dialog auto-closes when the Refresh check passes.
  }

  // ---------------------------------------------------------------------------
  // Inline bot-linking UI
  // (mirrors MessengerLinkDialog; success check uses notificationChannel, not contactOptions,
  // so users without a Telegram username are handled correctly)
  // ---------------------------------------------------------------------------

  private Component buildBotHandleRow(MessengerLinkContext ctx) {
    TextField handleField = new TextField(getTranslation("messenger.link.bot.field.label"));
    handleField.setValue(ctx.botHandle());
    handleField.setReadOnly(true);
    handleField.setWidthFull();
    handleField.setSuffixComponent(copyButton(ctx.botHandle(), "messenger.link.handle.copied"));

    VerticalLayout wrapper = new VerticalLayout();
    wrapper.setPadding(false);
    wrapper.setSpacing(false);
    wrapper.add(handleField);

    ctx.botDeepLink()
        .ifPresent(
            url -> {
              Button openBtn =
                  new Button(
                      getTranslation("messenger.link.bot.open.button"),
                      VaadinIcon.EXTERNAL_LINK.create());
              openBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
              openBtn.addClickListener(e -> UI.getCurrent().getPage().open(url, "_blank"));
              wrapper.add(openBtn);
            });

    return wrapper;
  }

  private Component buildCodeRow(MessengerLinkContext ctx) {
    TextField codeField = new TextField(getTranslation("messenger.link.code.field.label"));
    codeField.setValue(ctx.verificationCode());
    codeField.setReadOnly(true);
    codeField.setWidthFull();
    codeField.getStyle().set("font-size", "var(--lumo-font-size-xl)").set("font-weight", "bold");
    codeField.setSuffixComponent(copyButton(ctx.verificationCode(), "messenger.link.code.copied"));
    return codeField;
  }

  private Optional<Component> buildStepsSection(MessengerLinkContext ctx) {
    List<VerificationStep> steps = ctx.steps();
    if (steps.isEmpty()) return Optional.empty();

    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.setSpacing(false);

    Span label = new Span(getTranslation("messenger.link.steps.section.label", steps.size()));
    label
        .getStyle()
        .set("font-size", "var(--lumo-font-size-s)")
        .set("font-weight", "600")
        .set("color", "var(--lumo-secondary-text-color)")
        .set("text-transform", "uppercase")
        .set("letter-spacing", "0.05em")
        .set("padding-top", "var(--lumo-space-s)")
        .set("display", "block");
    section.add(label);

    for (int i = 0; i < steps.size(); i++) {
      section.add(buildStepPanel(steps.get(i), i));
    }
    return Optional.of(section);
  }

  private Details buildStepPanel(VerificationStep step, int index) {
    Details details = new Details();
    details.setSummaryText(getTranslation("messenger.link.step.label", index + 1));
    details.setOpened(index == 0);
    details.setWidthFull();

    VerticalLayout stepContent = new VerticalLayout();
    stepContent.setPadding(true);
    stepContent.setSpacing(true);

    Paragraph text = new Paragraph(getTranslation(step.messageKey()));
    text.getStyle().set("margin", "0");
    stepContent.add(text);

    Optional.ofNullable(step.pictureUrl())
        .ifPresent(
            url -> {
              Image img =
                  new Image(url, getTranslation("messenger.link.step.image.alt", index + 1));
              img.setMaxWidth("375px");
              img.setWidth("100%");
              img.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
              stepContent.add(img);
            });

    details.add(stepContent);
    return details;
  }

  private Component buildStatusRow(MessengerLinkContext ctx) {
    Span statusLine = new Span(getTranslation("messenger.link.status.waiting"));
    statusLine
        .getStyle()
        .set("color", "var(--lumo-secondary-text-color)")
        .set("font-size", "var(--lumo-font-size-s)");

    Button refreshBtn =
        new Button(getTranslation("messenger.link.refresh.button"), VaadinIcon.REFRESH.create());
    refreshBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    refreshBtn.addClickListener(
        e -> {
          // Check notificationChannel, not contactOptions. A user without a Telegram username
          // gets no ContactOption but does get NotificationChannel.Telegram written by the bot.
          boolean nowLinked =
              userDao
                  .find(userId)
                  .map(u -> u.getNotificationChannel() instanceof NotificationChannel.Telegram)
                  .orElse(false);

          if (nowLinked) {
            close();
            onChanged.run();
          } else {
            statusLine.setText(
                getTranslation("messenger.link.status.not.confirmed", ctx.botHandle()));
            statusLine.getStyle().set("color", "var(--lumo-error-color)");
          }
        });

    HorizontalLayout row = new HorizontalLayout(statusLine, refreshBtn);
    row.setAlignItems(FlexComponent.Alignment.CENTER);
    row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
    row.setWidthFull();
    return row;
  }

  // ---------------------------------------------------------------------------
  // Shared helpers
  // ---------------------------------------------------------------------------

  private Button copyButton(String value, String confirmationKey) {
    Button btn = new Button(VaadinIcon.COPY.create());
    btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    btn.addClickListener(
        e -> {
          btn.getElement().executeJs("window.navigator.clipboard.writeText($0)", value);
          Notification.show(getTranslation(confirmationKey));
        });
    return btn;
  }
}

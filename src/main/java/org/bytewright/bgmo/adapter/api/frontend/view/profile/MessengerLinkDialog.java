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
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bytewright.bgmo.domain.model.notification.MessengerLinkContext;
import org.bytewright.bgmo.domain.model.notification.NotificationChannel;
import org.bytewright.bgmo.domain.model.notification.VerificationStep;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;

/**
 * Dialog that guides a user through linking a messenger account as a contact option.
 *
 * <p>Shows the bot handle (copyable + optional direct-open link), the verification code (copyable),
 * optional step-by-step tutorial panels, and a refresh button to check if the bot has confirmed the
 * linking.
 *
 * <p>Stays deliberately dumb: all data arrives pre-built in a {@link MessengerLinkContext}.
 *
 * <p>Success detection checks both the presence of a verified ContactOption <em>and</em> whether
 * {@code notificationChannel} is now Telegram. The latter handles users without a Telegram
 * username, who get no ContactOption but do get {@code NotificationChannel.Telegram} written by the
 * bot adapter.
 */
public class MessengerLinkDialog extends Dialog {

  private final MessengerLinkContext ctx;
  private final UUID currentUserId;
  private final RegisteredUserDao userDao;
  private final Runnable onLinked;

  public MessengerLinkDialog(
      MessengerLinkContext ctx, UUID currentUserId, RegisteredUserDao userDao, Runnable onLinked) {
    this.ctx = ctx;
    this.currentUserId = currentUserId;
    this.userDao = userDao;
    this.onLinked = onLinked;

    setHeaderTitle(getTranslation(ctx.type().getNameMessageKey()));
    setWidth("420px");
    setCloseOnOutsideClick(true);

    VerticalLayout content = new VerticalLayout();
    content.setPadding(false);
    content.setSpacing(true);

    content.add(buildBotHandleRow());
    content.add(buildCodeRow());
    buildStepsSection().ifPresent(content::add);
    content.add(buildStatusRow());

    add(content);

    Button closeBtn = new Button(VaadinIcon.CLOSE.create(), e -> close());
    closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    getHeader().add(closeBtn);
  }

  private Component buildBotHandleRow() {
    TextField handleField = new TextField(getTranslation("messenger.link.bot.field.label"));
    handleField.setValue(ctx.botHandle());
    handleField.setReadOnly(true);
    handleField.setWidthFull();

    Button copyBtn = copyButton(ctx.botHandle(), "messenger.link.handle.copied");
    handleField.setSuffixComponent(copyBtn);

    VerticalLayout wrapper = new VerticalLayout();
    wrapper.setPadding(false);
    wrapper.setSpacing(false);
    wrapper.add(handleField);
    Optional<String> deeplinkOpt = ctx.botDeepLink();
    if (deeplinkOpt.isPresent()) {
      String url = deeplinkOpt.get();
      Button openBtn =
          new Button(
              getTranslation("messenger.link.bot.open.button"), VaadinIcon.EXTERNAL_LINK.create());
      openBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
      openBtn.addClickListener(e -> UI.getCurrent().getPage().open(url, "_blank"));
      wrapper.add(openBtn);
    }
    return wrapper;
  }

  private Component buildCodeRow() {
    TextField codeField = new TextField(getTranslation("messenger.link.code.field.label"));
    codeField.setValue(ctx.verificationCode());
    codeField.setReadOnly(true);
    codeField.setWidthFull();
    codeField.getStyle().set("font-size", "var(--lumo-font-size-xl)").set("font-weight", "bold");

    codeField.setSuffixComponent(copyButton(ctx.verificationCode(), "messenger.link.code.copied"));
    return codeField;
  }

  private Optional<Component> buildStepsSection() {
    List<VerificationStep> steps = ctx.steps();
    if (steps.isEmpty()) return Optional.empty();
    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.setSpacing(false);

    Span sectionLabel =
        new Span(getTranslation("messenger.link.steps.section.label", steps.size()));
    sectionLabel
        .getStyle()
        .set("font-size", "var(--lumo-font-size-s)")
        .set("font-weight", "600")
        .set("color", "var(--lumo-secondary-text-color)")
        .set("text-transform", "uppercase")
        .set("letter-spacing", "0.05em")
        .set("padding-top", "var(--lumo-space-s)")
        .set("display", "block");
    section.add(sectionLabel);

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

  private Component buildStatusRow() {
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
          boolean nowLinked =
              userDao.find(currentUserId).map(u -> isLinked(u, ctx.type())).orElse(false);

          if (nowLinked) {
            close();
            onLinked.run();
          } else {
            statusLine.setText(
                getTranslation("messenger.link.status.not.confirmed", ctx.botHandle()));
            statusLine.getStyle().set("color", "var(--lumo-error-color)");
          }
        });

    HorizontalLayout footer = new HorizontalLayout(statusLine, refreshBtn);
    footer.setAlignItems(FlexComponent.Alignment.CENTER);
    footer.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
    footer.setWidthFull();
    return footer;
  }

  /**
   * A Telegram linking attempt is considered successful if either:
   *
   * <ul>
   *   <li>a verified ContactOption of the expected type exists (user has a Telegram username), or
   *   <li>the notification channel is now Telegram (user has no username; no ContactOption is
   *       created by the bot adapter in that case, but the channel is still written).
   * </ul>
   *
   * For Signal (and any future type), only the ContactOption check applies.
   */
  private boolean isLinked(
      org.bytewright.bgmo.domain.model.user.RegisteredUser user, ContactInfoType type) {
    boolean hasContactOption =
        user.getContactOptions().stream().anyMatch(c -> c.getType() == type && c.isVerified());
    boolean hasNotificationChannel =
        type == ContactInfoType.TELEGRAM
            && user.getNotificationChannel() instanceof NotificationChannel.Telegram;
    return hasContactOption || hasNotificationChannel;
  }

  /** Read-only copy-to-clipboard icon button for the given value. */
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

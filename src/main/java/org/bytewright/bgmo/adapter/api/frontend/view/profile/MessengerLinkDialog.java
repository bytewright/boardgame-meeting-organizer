package org.bytewright.bgmo.adapter.api.frontend.view.profile;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;

/**
 * Dialog that guides a user through linking a messenger account. Shows the verification code and
 * instructions, plus a refresh button to check if the bot has confirmed the linking.
 */
public class MessengerLinkDialog extends Dialog {

  private final RegisteredUser currentUser;
  private final RegisteredUserDao userDao;
  private final Runnable onLinked;

  public MessengerLinkDialog(
      ContactInfoType type,
      String verificationCode,
      String botHandle,
      RegisteredUser currentUser,
      RegisteredUserDao userDao,
      Runnable onLinked) {
    this.currentUser = currentUser;
    this.userDao = userDao;
    this.onLinked = onLinked;

    setHeaderTitle(messengerName(type));
    setWidth("400px");
    setCloseOnOutsideClick(true);

    VerticalLayout content = new VerticalLayout();
    content.setPadding(false);
    content.setSpacing(true);

    // Instruction text
    Paragraph instruction = new Paragraph();
    instruction
        .getElement()
        .setProperty(
            "innerHTML",
            "To link your %s account, open <b>%s</b> in %s and send it the following code:"
                .formatted(messengerName(type), botHandle, messengerName(type)));
    instruction.getStyle().set("margin", "0");

    // Code display field with copy button
    TextField codeField = new TextField();
    codeField.setValue(verificationCode);
    codeField.setReadOnly(true);
    codeField.setWidthFull();
    codeField
        .getStyle()
        .set("font-size", "var(--lumo-font-size-xl)")
        .set("font-weight", "bold")
        .set("letter-spacing", "0.15em");

    Button copyBtn = new Button(VaadinIcon.COPY.create());
    copyBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    copyBtn.addClickListener(
        e -> {
          codeField
              .getElement()
              .executeJs("window.navigator.clipboard.writeText($0)", verificationCode);
          Notification.show("Code copied!");
        });
    codeField.setSuffixComponent(copyBtn);

    // Status line
    Span statusLine = new Span("Waiting for confirmation...");
    statusLine
        .getStyle()
        .set("color", "var(--lumo-secondary-text-color)")
        .set("font-size", "var(--lumo-font-size-s)");

    // Refresh button — re-checks whether the bot has confirmed
    Button refreshBtn = new Button("Check status", VaadinIcon.REFRESH.create());
    refreshBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    refreshBtn.addClickListener(
        e -> {
          boolean nowLinked =
              userDao
                  .find(currentUser.getId())
                  .map(
                      u ->
                          u.getContactInfos().stream()
                              .anyMatch(c -> c.type() == type && c.isVerified()))
                  .orElse(false);

          if (nowLinked) {
            close();
            onLinked.run();
          } else {
            statusLine.setText("Not confirmed yet — make sure you sent the code to " + botHandle);
            statusLine.getStyle().set("color", "var(--lumo-error-color)");
          }
        });

    HorizontalLayout footer = new HorizontalLayout(statusLine, refreshBtn);
    footer.setAlignItems(FlexComponent.Alignment.CENTER);
    footer.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
    footer.setWidthFull();

    content.add(instruction, codeField, footer);
    add(content);

    // Close button in dialog header
    Button closeBtn = new Button(VaadinIcon.CLOSE.create(), e -> close());
    closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    getHeader().add(closeBtn);
  }

  private static String messengerName(ContactInfoType type) {
    return switch (type) {
      case TELEGRAM -> "Telegram";
      case SIGNAL -> "Signal";
      default -> type.name();
    };
  }
}

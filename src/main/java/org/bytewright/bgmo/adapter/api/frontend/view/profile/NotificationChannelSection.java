package org.bytewright.bgmo.adapter.api.frontend.view.profile;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.bytewright.bgmo.adapter.api.frontend.view.component.factory.ComponentFactory;
import org.bytewright.bgmo.domain.model.notification.NotificationChannel;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.usecases.NotificationWorkflows;

/**
 * Displays the user's current notification channel and provides buttons to change it or send a test
 * message.
 *
 * <p>Intentionally stateless: the parent view passes a freshly-loaded {@link RegisteredUser} on
 * each rebuild, and {@code onChanged} triggers a full view rebuild so this component is simply
 * replaced rather than updated in place.
 */
public class NotificationChannelSection extends VerticalLayout {

  public NotificationChannelSection(
      ComponentFactory componentFactory,
      NotificationWorkflows notificationWorkflows,
      RegisteredUser user,
      Runnable onChanged) {

    setPadding(true);
    setSpacing(true);
    getStyle()
        .set("background-color", "var(--lumo-contrast-5pct)")
        .set("border-radius", "var(--lumo-border-radius-m)")
        .set("margin-bottom", "var(--lumo-space-m)");

    H3 heading = new H3(getTranslation("profile.notifications.channel.title"));
    heading.getStyle().set("margin", "0 0 var(--lumo-space-s) 0");
    add(heading);

    Span statusSpan = new Span(describeChannel(user.getNotificationChannel()));
    statusSpan.getStyle().set("font-size", "var(--lumo-font-size-s)");

    Button changeBtn = new Button(getTranslation("profile.notifications.channel.change"));
    changeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    changeBtn.addClickListener(
        e -> componentFactory.notificationChannelDialog(user, onChanged).open());

    HorizontalLayout row = new HorizontalLayout(statusSpan, changeBtn);
    row.setAlignItems(Alignment.CENTER);
    row.setWidthFull();

    if (!(user.getNotificationChannel() instanceof NotificationChannel.None)) {
      Button testBtn = new Button(getTranslation("profile.notifications.channel.test"));
      testBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
      testBtn.addClickListener(
          e -> {
            notificationWorkflows.sendTestMessage(user.getId());
            Notification n =
                Notification.show(getTranslation("profile.notifications.channel.test.sent"));
            n.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
          });
      row.add(testBtn);
    }

    add(row);
  }

  private String describeChannel(NotificationChannel channel) {
    return switch (channel) {
      case NotificationChannel.None ignored ->
          getTranslation("profile.notifications.channel.status.none");
      case NotificationChannel.Email e ->
          getTranslation("profile.notifications.channel.status.email", e.email());
      case NotificationChannel.Telegram ignored ->
          getTranslation("profile.notifications.channel.status.telegram");
    };
  }
}

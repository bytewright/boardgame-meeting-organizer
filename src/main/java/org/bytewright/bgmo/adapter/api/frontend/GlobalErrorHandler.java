package org.bytewright.bgmo.adapter.api.frontend;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.server.DefaultErrorHandler;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.spring.annotation.SpringComponent;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringComponent
public class GlobalErrorHandler extends DefaultErrorHandler {

  @Override
  public void error(ErrorEvent event) {
    Throwable throwable = event.getThrowable();

    // Find the root cause
    while (throwable.getCause() != null) {
      throwable = throwable.getCause();
    }

    // Handle NoSuchElementException specifically
    if (throwable instanceof NoSuchElementException) {
      log.warn("An Optional value was not present when required", throwable);

      // Show user-friendly notification on UI thread
      UI currentUI = UI.getCurrent();
      if (currentUI != null && currentUI.isAttached()) {
        currentUI.access(
            () -> {
              Notification notification =
                  Notification.show(
                      "The requested item was not found.", 3000, Notification.Position.MIDDLE);
              notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            });
      }
    } else {
      super.error(event);
      // // Handle other exceptions
      // log.error("Unhandled exception", throwable);
      //
      // // Default error handling for other exceptions
      // UI currentUI = UI.getCurrent();
      // if (currentUI != null && currentUI.isAttached()) {
      //   currentUI.access(
      //       () -> {
      //         Notification notification =
      //             Notification.show(
      //                 "An error occurred. Please try again later.",
      //                 5000,
      //                 Notification.Position.MIDDLE);
      //         notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
      //       });
      // }
    }
  }
}

package org.bytewright.bgmo.adapter.api.frontend.view.meetup.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.dom.Style;
import org.bytewright.bgmo.adapter.api.frontend.service.i18n.LocaleService;
import org.bytewright.bgmo.adapter.api.frontend.view.meetup.model.MeetupAttendeesContext;
import org.bytewright.bgmo.adapter.api.frontend.view.meetup.model.MeetupAttendeesContext.AttendeeRequestItem;
import org.bytewright.bgmo.domain.model.JoinRequestPayload;
import org.bytewright.bgmo.domain.model.RequestState;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.usecases.AdminWorkflows;
import org.bytewright.bgmo.usecases.MeetupWorkflows;

public class AttendeeRequestCard extends Div {

  private static final String BORDER = "1px solid var(--lumo-contrast-20pct)";
  private static final String PAD_CELL = "var(--lumo-space-s) var(--lumo-space-m)";

  public AttendeeRequestCard(
      MeetupAttendeesContext ctx,
      AttendeeRequestItem item,
      boolean isAdmin,
      RegisteredUserDao userDao,
      MeetupWorkflows meetupWorkflows,
      AdminWorkflows adminWorkflows,
      LocaleService localeService,
      Runnable onRefresh) {

    var request = item.request();
    RequestState state = request.getRequestState();

    // ── Outer card shell ─────────────────────────────────────────────────────
    getStyle()
        .set("border", BORDER)
        .set("border-radius", "var(--lumo-border-radius-m)")
        .set("background", "var(--lumo-base-color)")
        .set("display", "flex")
        .set("flex-direction", "column")
        .set("width", "100%")
        .set("overflow", "hidden"); // keeps children inside rounded corners

    Div headerRow = new Div();
    headerRow
        .getStyle()
        .setDisplay(Style.Display.GRID)
        .set("grid-template-columns", "auto 1fr auto")
        .setBorderBottom(BORDER);

    Div dotCell = gridCell(true);
    dotCell
        .getStyle()
        .setDisplay(Style.Display.FLEX)
        .setAlignItems(Style.AlignItems.CENTER)
        .setJustifyContent(Style.JustifyContent.CENTER);
    dotCell.add(statusDot(state));

    Div regTypeCell = gridCell(true);
    Span regTypeSpan =
        new Span(
            JoinRequestPayload.isUser(request.getPayload())
                ? getTranslation("meetup.attendees.type.registered")
                : getTranslation("meetup.attendees.type.anon"));
    regTypeSpan
        .getStyle()
        .set("color", "var(--lumo-secondary-text-color)")
        .set("font-size", "var(--lumo-font-size-s)")
        .setWhiteSpace(Style.WhiteSpace.NOWRAP);
    regTypeCell.add(regTypeSpan);

    Div timeCell = gridCell(false);
    Span timeSpan = new Span(localeService.formatRelative(request.getTsCreation()));
    timeSpan
        .getStyle()
        .set("color", "var(--lumo-secondary-text-color)")
        .set("font-size", "var(--lumo-font-size-s)")
        .setWhiteSpace(Style.WhiteSpace.NOWRAP);
    timeCell.add(timeSpan);

    headerRow.add(dotCell, regTypeCell, timeCell);
    add(headerRow);

    Div nameRow = fullWidthRow(true);
    String displayName =
        switch (request.getPayload()) {
          case JoinRequestPayload.Anon anon -> anon.displayName();
          case JoinRequestPayload.User user ->
              userDao.findById(user.userId()).map(RegisteredUser::getDisplayName).orElse("-");
        };

    Span nameTitle = new Span(getTranslation("meetup.attendees.name"));
    Span nameSpan = new Span(displayName);
    nameSpan
        .getStyle()
        .set("font-weight", "bold")
        .set("display", "block")
        .set("overflow-wrap", "break-word");
    nameRow.add(new HorizontalLayout(nameTitle, nameSpan));
    add(nameRow);

    Div contactRow = fullWidthRow(true);
    Span contactLabel = new Span(getTranslation("meetup.attendees.contact") + ": ");
    contactLabel
        .getStyle()
        .set("color", "var(--lumo-secondary-text-color)")
        .set("font-size", "var(--lumo-font-size-s)");
    Span contactValue = item.resolvedContact();
    contactValue.getStyle().set("word-break", "break-word");
    contactRow.add(contactLabel, contactValue);
    add(contactRow);

    String comment = request.getComment();
    if (comment != null && !comment.isBlank()) {
      Div commentRow = fullWidthRow(true);
      Span commentLabel = new Span(getTranslation("meetup.attendees.comment") + ": ");
      commentLabel
          .getStyle()
          .set("color", "var(--lumo-secondary-text-color)")
          .set("font-size", "var(--lumo-font-size-s)");
      Span commentValue = new Span(comment);
      commentValue.getStyle().set("word-break", "break-word").set("font-style", "italic");
      commentRow.add(commentLabel, commentValue);
      add(commentRow);
    }

    add(buildButtonRow(ctx, item, isAdmin, meetupWorkflows, adminWorkflows, onRefresh));
  }

  private HorizontalLayout buildButtonRow(
      MeetupAttendeesContext ctx,
      AttendeeRequestItem item,
      boolean isAdmin,
      MeetupWorkflows meetupWorkflows,
      AdminWorkflows adminWorkflows,
      Runnable onRefresh) {

    var request = item.request();
    RequestState state = request.getRequestState();
    boolean isOpen = state == RequestState.OPEN;
    boolean isAccepted = state == RequestState.ACCEPTED;
    boolean isCanceledOrDeclined = state == RequestState.CANCELED || state == RequestState.DECLINED;

    HorizontalLayout row = new HorizontalLayout();
    row.setWidthFull();
    row.setSpacing(false);
    row.setAlignItems(FlexComponent.Alignment.CENTER);
    row.getStyle().set("border-top", BORDER).set("padding", "var(--lumo-space-xs)");

    // Confirm — full-width on organiser, expands to fill on admin
    Button confirmBtn =
        new Button(
            getTranslation("meetup.confirm"),
            e -> {
              try {
                meetupWorkflows.confirmAttendee(ctx.meetupId(), request);
                Notification.show(
                    getTranslation("meetup.attendeeConfirmed"),
                    2000,
                    Notification.Position.BOTTOM_START);
                onRefresh.run();
              } catch (Exception ex) {
                showError(ex.getMessage());
              }
            });
    confirmBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
    confirmBtn.setEnabled(isOpen && !ctx.isMeetupCanceled());
    row.add(confirmBtn);

    if (isAdmin) {
      Button revokeBtn =
          new Button(
              getTranslation("meetup.attendees.revoke"),
              e -> {
                try {
                  adminWorkflows.revokeAttendeeConfirmation(request.getId());
                  Notification.show(
                      getTranslation("meetup.attendees.revokeConfirmed"),
                      2000,
                      Notification.Position.BOTTOM_START);
                  onRefresh.run();
                } catch (Exception ex) {
                  showError(ex.getMessage());
                }
              });
      revokeBtn.addThemeVariants(ButtonVariant.LUMO_WARNING, ButtonVariant.LUMO_SMALL);
      revokeBtn.getStyle().set("border-left", BORDER);
      revokeBtn.setEnabled(isAccepted);
      row.add(revokeBtn);

      Button deleteBtn =
          new Button(
              getTranslation("meetup.attendees.delete"),
              e -> {
                try {
                  adminWorkflows.deleteJoinRequest(request.getId());
                  Notification.show(
                      getTranslation("meetup.attendees.deleteConfirmed"),
                      2000,
                      Notification.Position.BOTTOM_START);
                  onRefresh.run();
                } catch (Exception ex) {
                  showError(ex.getMessage());
                }
              });
      deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
      deleteBtn.getStyle().set("border-left", BORDER);
      deleteBtn.setEnabled(!isCanceledOrDeclined);
      row.add(deleteBtn);
    }

    row.expand(confirmBtn);
    return row;
  }

  // ── Status dot ────────────────────────────────────────────────────────────

  private Span statusDot(RequestState state) {
    String color =
        switch (state) {
          case OPEN -> "var(--lumo-warning-color)";
          case ACCEPTED -> "var(--lumo-success-color)";
          case DECLINED, CANCELED -> "var(--lumo-contrast-40pct)";
        };
    String tooltip =
        switch (state) {
          case OPEN -> getTranslation("meetup.joinStatusPending");
          case ACCEPTED -> getTranslation("meetup.joinStatusConfirm");
          case DECLINED -> getTranslation("meetup.joinStatusDeclined");
          case CANCELED -> getTranslation("meetup.joinStatusUserCanceled");
        };
    Span dot = new Span();
    dot.setTitle(tooltip);
    dot.getStyle()
        .set("display", "inline-block")
        .set("width", "12px")
        .set("height", "12px")
        .set("border-radius", "50%")
        .set("background-color", color)
        .set("flex-shrink", "0");
    return dot;
  }

  // ── Layout helpers ────────────────────────────────────────────────────────

  /** A grid cell with optional right border, standard padding. */
  private static Div gridCell(boolean rightBorder) {
    Div cell = new Div();
    cell.getStyle().setPadding(PAD_CELL);
    if (rightBorder) {
      cell.getStyle().setBorderRight(BORDER);
    }
    return cell;
  }

  /** A full-width row with standard padding and optional bottom border. */
  private static Div fullWidthRow(boolean bottomBorder) {
    Div row = new Div();
    row.setWidthFull();
    row.getStyle().setPadding(PAD_CELL).set("box-sizing", "border-box");
    if (bottomBorder) {
      row.getStyle().setBorderBottom(BORDER);
    }
    return row;
  }

  private void showError(String message) {
    Notification n = Notification.show(message, 4000, Notification.Position.MIDDLE);
    n.addThemeVariants(NotificationVariant.LUMO_ERROR);
  }
}

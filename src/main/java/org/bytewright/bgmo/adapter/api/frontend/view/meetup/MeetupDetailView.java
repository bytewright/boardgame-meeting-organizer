package org.bytewright.bgmo.adapter.api.frontend.view.meetup;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.api.frontend.SessionAuthenticationService;
import org.bytewright.bgmo.adapter.api.frontend.service.MeetupDetailContext;
import org.bytewright.bgmo.adapter.api.frontend.service.MeetupDetailContextBuilder;
import org.bytewright.bgmo.adapter.api.frontend.service.i18n.LocaleService;
import org.bytewright.bgmo.adapter.api.frontend.view.DashboardView;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.adapter.api.frontend.view.meetup.component.*;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.bytewright.bgmo.usecases.MeetupWorkflows;

/**
 * Public detail page for a single meetup event.
 *
 * <p>This class is intentionally thin — it is responsible only for:
 *
 * <ol>
 *   <li>Resolving the meetup from the route parameter.
 *   <li>Reading the visitor's identity (logged-in user or anon session token).
 *   <li>Delegating to {@link MeetupDetailContextBuilder} to compute the view model.
 *   <li>Assembling shared components and exactly one role-specific panel.
 * </ol>
 *
 * <p>All display logic lives in the individual components. All domain operations go through {@link
 * MeetupWorkflows}. See {@link MeetupDetailContext} and {@link ViewerRole} for the access model.
 *
 * <p>Anonymous session tokens are managed here (via {@code VaadinSession}) and passed into {@link
 * AnonPanel} as lambdas to avoid spreading session access across classes.
 */
@Slf4j
@Route(value = "meetup/:meetupId", layout = MainLayout.class)
@PageTitle("Meetup | " + APP_NAME_SHORT)
@AnonymousAllowed
public class MeetupDetailView extends VerticalLayout implements BeforeEnterObserver {

  private static final String ANON_TOKEN_KEY_PREFIX = "anonToken:meetup:";

  private final LocaleService localeService;
  private final SessionAuthenticationService authService;
  private final MeetupWorkflows meetupWorkflows;
  private final MeetupDetailContextBuilder contextBuilder;
  private final MeetupDao meetupDao;

  private RegisteredUser currentUser;
  private MeetupEvent meetup;
  private MeetupDetailContext ctx;

  public MeetupDetailView(
      LocaleService localeService,
      SessionAuthenticationService authService,
      MeetupWorkflows meetupWorkflows,
      MeetupDetailContextBuilder contextBuilder,
      MeetupDao meetupDao) {
    this.localeService = localeService;
    this.authService = authService;
    this.meetupWorkflows = meetupWorkflows;
    this.contextBuilder = contextBuilder;
    this.meetupDao = meetupDao;
    setMaxWidth(MainLayout.MAX_DISPLAYPORT_WIDTH);
    getStyle().set("margin", "0 auto");
  }

  // ── UI construction ───────────────────────────────────────────────────────

  private void buildUI() {
    removeAll();

    // ── Shared components (always visible) ────────────────────────────────────
    add(new MeetupInfoHeader(ctx, localeService));
    add(new Hr());

    if (!ctx.offeredGames().isEmpty()) {
      add(new OfferedGamesSection(ctx));
      add(new Hr());
    }

    add(new ConfirmedAttendeesSection(ctx));
    add(new Hr());

    // ── Role-specific panel (exactly one) ─────────────────────────────────────
    add(
        switch (ctx.role()) {
          case ANONYMOUS, ANON_PENDING, ANON_ACCEPTED ->
              new AnonPanel(ctx, meetupWorkflows, this::getOrCreateAnonSessionToken, this::refresh);
          case REGISTERED_CANCELED, REGISTERED_PENDING, REGISTERED_ACCEPTED ->
              new GuestPanel(ctx, meetupWorkflows, this::refresh);
          case ORGANIZER -> new OrganizerPanel(ctx, meetupWorkflows, this::refresh);
        });
  }

  // ── Refresh ───────────────────────────────────────────────────────────────

  private void refresh() {
    this.meetup = meetupDao.findOrThrow(meetup.getId());
    this.ctx = contextBuilder.build(meetup, currentUser, getAnonSessionToken());
    buildUI();
  }

  // ── Anonymous session token helpers ───────────────────────────────────────

  private UUID getAnonSessionToken() {
    return (UUID) VaadinSession.getCurrent().getAttribute(anonTokenKey());
  }

  private UUID getOrCreateAnonSessionToken() {
    UUID existing = getAnonSessionToken();
    if (existing != null) {
      return existing;
    }
    UUID token = UUID.randomUUID();
    VaadinSession.getCurrent().setAttribute(anonTokenKey(), token);
    return token;
  }

  private String anonTokenKey() {
    return ANON_TOKEN_KEY_PREFIX + meetup.getId();
  }

  // ── BeforeEnter ───────────────────────────────────────────────────────────

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    this.currentUser = authService.getCurrentUser().orElse(null);

    String meetupIdParam = event.getRouteParameters().get("meetupId").orElse(null);
    if (meetupIdParam == null) {
      log.info("MeetupDetailView opened without meetupId — forwarding to dashboard");
      event.forwardTo(DashboardView.class);
      return;
    }

    try {
      this.meetup = meetupDao.findOrThrow(UUID.fromString(meetupIdParam));
    } catch (Exception e) {
      log.warn("Meetup not found or invalid UUID: {}", meetupIdParam);
      event.forwardTo(DashboardView.class);
      return;
    }

    this.ctx = contextBuilder.build(meetup, currentUser, getAnonSessionToken());
    buildUI();
  }
}

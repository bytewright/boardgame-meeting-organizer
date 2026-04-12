package org.bytewright.bgmo.adapter.api.frontend.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.PermitAll;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.api.frontend.SessionAuthenticationService;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.MeetupJoinRequest;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.bytewright.bgmo.domain.service.data.RegisteredUserDao;
import org.bytewright.bgmo.usecases.MeetupWorkflows;

@Slf4j
@Route("meetup/:meetupId")
@PageTitle("Meetup Detail | Boardgame Meeting Organizer")
@PermitAll
public class MeetupDetailView extends VerticalLayout implements BeforeEnterObserver {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy 'at' HH:mm", Locale.ENGLISH);

    private final SessionAuthenticationService authService;
    private final MeetupWorkflows meetupWorkflows;
    private final MeetupDao meetupDao;
    private final RegisteredUserDao userDao;

    private RegisteredUser currentUser;
    private MeetupEvent meetup;

    public MeetupDetailView(
            SessionAuthenticationService authService,
            MeetupWorkflows meetupWorkflows,
            MeetupDao meetupDao,
            RegisteredUserDao userDao) {
        this.authService = authService;
        this.meetupWorkflows = meetupWorkflows;
        this.meetupDao = meetupDao;
        this.userDao = userDao;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    private void buildUI() {
        removeAll();

        // ── Back navigation ────────────────────────────────────────────────────
        Button backBtn =
                new Button("← Back to Dashboard", e -> UI.getCurrent().navigate(DashboardView.class));
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        add(backBtn);

        // ── Canceled banner ────────────────────────────────────────────────────
        if (meetup.isCanceled()) {
            Span canceledBadge = new Span("⚠ This meetup has been CANCELED");
            canceledBadge
                    .getStyle()
                    .set("color", "var(--lumo-error-color)")
                    .set("font-weight", "bold")
                    .set("font-size", "1.1em");
            add(canceledBadge);
        }

        // ── Event info ─────────────────────────────────────────────────────────
        H2 titleHeading = new H2(meetup.getTitle());

        Paragraph description = new Paragraph(
                meetup.getDescription() != null && !meetup.getDescription().isBlank()
                        ? meetup.getDescription()
                        : "No description provided.");

        Span dateSpan = new Span("📅 " + meetup.getEventDate().format(DATE_FMT));
        Span durationSpan = new Span("⏱ " + meetup.getDurationHours() + " hour(s)");

        String slotsText =
                meetup.isUnlimitedSlots()
                        ? "∞ unlimited slots"
                        : meetup.getConfirmedAttendeeIds().size() + " / " + meetup.getJoinSlots() + " slots filled";
        Span slotsSpan = new Span("👥 " + slotsText);

        VerticalLayout infoSection = new VerticalLayout(titleHeading, description, dateSpan, durationSpan, slotsSpan);
        infoSection.setPadding(false);
        infoSection.setSpacing(true);
        add(infoSection);

        add(new Hr());

        // ── Role-specific section ─────────────────────────────────────────────
        boolean isOwner = meetup.getCreatorId().equals(currentUser.getId());
        if (isOwner) {
            buildOwnerSection();
        } else {
            buildGuestSection();
        }
    }

    // ── Guest view ────────────────────────────────────────────────────────────

    private void buildGuestSection() {
        boolean alreadyRequested =
                meetup.getJoinRequests().stream()
                        .anyMatch(r -> r.getUserId().equals(currentUser.getId()));
        boolean alreadyConfirmed = meetup.getConfirmedAttendeeIds().contains(currentUser.getId());
        boolean isFull =
                !meetup.isUnlimitedSlots()
                && meetup.getConfirmedAttendeeIds().size() >= meetup.getJoinSlots();

        if (alreadyConfirmed) {
            Span status = new Span("✓ You are confirmed for this meetup!");
            status
                    .getStyle()
                    .set("color", "var(--lumo-success-color)")
                    .set("font-weight", "bold")
                    .set("font-size", "1.1em");
            add(status);
        } else if (alreadyRequested) {
            Span status = new Span("⏳ Your join request is awaiting the organiser's approval.");
            status.getStyle().set("color", "var(--lumo-primary-color)");
            add(status);
        } else {
            Button joinBtn =
                    new Button(
                            isFull ? "Event is full" : "Request to Join",
                            e -> {
                                meetupWorkflows.requestToJoin(meetup.getId(), currentUser.getId(), null);
                                Notification n =
                                        Notification.show(
                                                "Join request sent!", 3000, Notification.Position.TOP_CENTER);
                                n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                                refreshMeetup();
                            });
            joinBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
            joinBtn.setEnabled(!meetup.isCanceled() && !isFull);
            add(joinBtn);
        }
    }

    // ── Owner view ────────────────────────────────────────────────────────────

    private void buildOwnerSection() {
        add(new H3("Join Requests"));

        List<MeetupJoinRequest> requests = meetup.getJoinRequests();
        Set<UUID> confirmedIds = meetup.getConfirmedAttendeeIds();

        if (requests.isEmpty()) {
            add(new Span("No join requests yet."));
        } else {
            Grid<MeetupJoinRequest> requestGrid = new Grid<>();
            requestGrid
                    .addColumn(req -> resolveUserName(req.getUserId()))
                    .setHeader("User")
                    .setFlexGrow(1);
            requestGrid
                    .addColumn(
                            req -> confirmedIds.contains(req.getUserId()) ? "✓ Confirmed" : "Pending")
                    .setHeader("Status")
                    .setAutoWidth(true);
            requestGrid
                    .addComponentColumn(
                            req -> {
                                boolean isConfirmed = confirmedIds.contains(req.getUserId());
                                Button confirmBtn =
                                        new Button(
                                                isConfirmed ? "Confirmed" : "Confirm",
                                                e -> {
                                                    try {
                                                        meetupWorkflows.confirmAttendees(
                                                                meetup.getId(), Set.of(req.getUserId()));
                                                        Notification.show(
                                                                resolveUserName(req.getUserId()) + " confirmed!",
                                                                2000,
                                                                Notification.Position.BOTTOM_START);
                                                        refreshMeetup();
                                                    } catch (IllegalArgumentException ex) {
                                                        Notification n =
                                                                Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
                                                        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                                                    }
                                                });
                                confirmBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
                                confirmBtn.setEnabled(!isConfirmed && !meetup.isCanceled());
                                return confirmBtn;
                            })
                    .setHeader("Action")
                    .setAutoWidth(true);

            requestGrid.setItems(requests);
            //requestGrid.setHeightByRows(true);
            add(requestGrid);
        }

        // Random attendee picker — only meaningful when slots are limited
        if (!meetup.isUnlimitedSlots()) {
            long pendingCount =
                    requests.stream()
                            .filter(r -> !confirmedIds.contains(r.getUserId()))
                            .count();
            int remainingSlots = Math.max(0, meetup.getJoinSlots() - confirmedIds.size());

            if (remainingSlots > 0 && pendingCount > 0) {
                Button randomBtn =
                        new Button(
                                "🎲 Choose Random Attendees for " + remainingSlots + " Remaining Slot(s)",
                                e -> confirmRandomAttendees(remainingSlots));
                randomBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                randomBtn.setEnabled(!meetup.isCanceled());
                add(randomBtn);
            } else if (remainingSlots == 0) {
                Span fullLabel = new Span("All slots are filled.");
                fullLabel.getStyle().set("color", "var(--lumo-success-color)").set("font-weight", "bold");
                add(fullLabel);
            }
        }
    }

    private void confirmRandomAttendees(int slotsLeft) {
        List<UUID> unconfirmedRequesters =
                meetup.getJoinRequests().stream()
                        .map(MeetupJoinRequest::getUserId)
                        .filter(uid -> !meetup.getConfirmedAttendeeIds().contains(uid))
                        .collect(Collectors.toCollection(ArrayList::new));

        if (unconfirmedRequesters.isEmpty()) {
            Notification.show(
                    "No pending requests to pick from.", 3000, Notification.Position.TOP_CENTER);
            return;
        }

        Collections.shuffle(unconfirmedRequesters);
        Set<UUID> toConfirm =
                new HashSet<>(unconfirmedRequesters.subList(0, Math.min(slotsLeft, unconfirmedRequesters.size())));

        try {
            meetupWorkflows.confirmAttendees(meetup.getId(), toConfirm);
            Notification n =
                    Notification.show(
                            toConfirm.size() + " attendee(s) randomly confirmed!",
                            3000,
                            Notification.Position.TOP_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshMeetup();
        } catch (IllegalArgumentException ex) {
            Notification n = Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String resolveUserName(UUID userId) {
        try {
            return userDao.findOrThrow(userId).getName();
        } catch (Exception e) {
            log.warn("Could not resolve name for userId {}", userId);
            return userId.toString();
        }
    }

    private void refreshMeetup() {
        this.meetup = meetupDao.findOrThrow(meetup.getId());
        buildUI();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<RegisteredUser> userOpt = authService.getCurrentUser();
        if (userOpt.isEmpty()) {
            event.forwardTo(LoginView.class);
            return;
        }
        this.currentUser = userOpt.get();

        String meetupIdParam = event.getRouteParameters().get("meetupId").orElse(null);
        if (meetupIdParam == null) {
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
        buildUI();
    }
}
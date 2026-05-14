package org.bytewright.bgmo.adapter.api.frontend.view;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.api.frontend.view.component.LocalePicker;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.adapter.api.frontend.view.component.factory.ComponentFactory;
import org.bytewright.bgmo.adapter.api.frontend.view.legal.DatenschutzView;
import org.bytewright.bgmo.adapter.api.frontend.view.profile.ContactSettingsView;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.security.AutoLoginService;
import org.bytewright.bgmo.domain.service.security.PasswordRules;
import org.bytewright.bgmo.usecases.UserWorkflows;

@Slf4j
@Route("register")
@PageTitle("Join " + APP_NAME_SHORT)
@AnonymousAllowed
public class RegistrationView extends VerticalLayout {
  private final AutoLoginService autoLoginService;
  private final UserWorkflows userWorkflows;
  private final ComponentFactory componentFactory;

  /** Captured when the form is rendered; used for the bot timestamp check. */
  private final Instant formRenderedAt = Instant.now();

  public RegistrationView(
      AutoLoginService autoLoginService,
      UserWorkflows userWorkflows,
      ComponentFactory componentFactory) {
    this.autoLoginService = autoLoginService;
    this.userWorkflows = userWorkflows;
    this.componentFactory = componentFactory;

    addClassName("registration-view");
    setAlignItems(Alignment.CENTER);
    setSizeFull();
    setPadding(true);
    setSpacing(true);
    setMaxWidth(MainLayout.MAX_DISPLAYPORT_WIDTH);
    getStyle().setMargin("0 auto");
    add(buildForm());
  }

  private VerticalLayout buildForm() {
    VerticalLayout card = new VerticalLayout();
    card.setMaxWidth(MainLayout.MAX_DISPLAYPORT_WIDTH);
    card.setWidthFull();
    card.setPadding(true);
    card.setSpacing(true);
    card.getStyle()
        .set("box-shadow", "var(--lumo-box-shadow-m)")
        .set("border-radius", "var(--lumo-border-radius-l)");

    // --- Header ---
    HorizontalLayout header = new HorizontalLayout();
    header.setWidthFull();
    header.setAlignItems(FlexComponent.Alignment.CENTER);
    header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

    H1 title = new H1(getTranslation("register.title"));
    title.getStyle().set("margin", "0").set("font-size", "var(--lumo-font-size-xxl)");
    LocalePicker localePicker = componentFactory.localePicker();
    header.add(title, localePicker);

    // --- Account Fields ---
    Binder<RegisteredUser.Creation.CreationBuilder> binder = new Binder<>();
    RegisteredUser.Creation.CreationBuilder dto = RegisteredUser.Creation.builder();

    TextField loginName = new TextField(getTranslation("register.field.loginName"));
    loginName.setWidthFull();
    loginName.setRequired(true);
    loginName.setHelperText(getTranslation("register.field.loginName.helper"));
    binder
        .forField(loginName)
        .asRequired(getTranslation("register.error.required"))
        .withValidator(v -> v.length() >= 3, getTranslation("register.error.name.tooShort"))
        .withValidator(
            userWorkflows::validateLoginName,
            getTranslation("register.error.loginName.alreadyTaken"))
        .bind(c -> "", RegisteredUser.Creation.CreationBuilder::loginName);

    TextField displayName = new TextField(getTranslation("register.field.displayName"));
    displayName.setWidthFull();
    displayName.setRequired(true);
    displayName.setHelperText(getTranslation("register.field.displayName.helper"));
    binder
        .forField(displayName)
        .asRequired(getTranslation("register.error.required"))
        .withValidator(v -> v.length() >= 3, getTranslation("register.error.name.tooShort"))
        .withValidator(
            userWorkflows::validateDisplayName,
            getTranslation("register.error.displayName.invalid")) // profanity filter
        .bind(c -> "", RegisteredUser.Creation.CreationBuilder::displayName);

    PasswordField password = new PasswordField(getTranslation("register.field.password"));
    password.setWidthFull();
    password.setRequired(true);
    password.setHelperText(
        getTranslation("register.field.password.helper", PasswordRules.PW_MIN_CHARS));
    binder
        .forField(password)
        .asRequired(getTranslation("register.error.required"))
        .withValidator(
            v -> v.length() >= PasswordRules.PW_MIN_CHARS,
            getTranslation("register.error.password.tooShort"))
        .bind(c -> "", RegisteredUser.Creation.CreationBuilder::password);

    PasswordField passwordConfirm =
        new PasswordField(getTranslation("register.field.passwordConfirm"));
    passwordConfirm.setWidthFull();
    passwordConfirm.setRequired(true);
    binder
        .forField(passwordConfirm)
        .withValidator(
            v -> v.equals(password.getValue()), getTranslation("register.error.password.mismatch"))
        .bind(c -> "", (c, v) -> {});

    // ── Honeypot (bot trap) ───────────────────────────────────────────────────
    // Hidden from real users via CSS; bots that auto-fill all fields will trigger this.
    TextField honeypot = new TextField();
    honeypot.setTabIndex(-1);
    honeypot.getElement().setAttribute("aria-hidden", "true");
    honeypot.getElement().setAttribute("autocomplete", "off");
    honeypot
        .getStyle()
        .set("position", "absolute")
        .set("left", "-9999px")
        .set("width", "1px")
        .set("height", "1px")
        .set("overflow", "hidden")
        .set("opacity", "0");

    // ── Divider ───────────────────────────────────────────────────────────────
    Hr divider = new Hr();

    // ── Contact info explainer ────────────────────────────────────────────────
    Component contactExplainerBox = buildContactExplainerBox();

    // ── GDPR consent ─────────────────────────────────────────────────────────
    Hr divider2 = new Hr();

    Paragraph consentNotice = new Paragraph(getTranslation("register.consent.notice"));
    consentNotice
        .getStyle()
        .set("font-size", "var(--lumo-font-size-s)")
        .set("color", "var(--lumo-secondary-text-color)")
        .set("margin-bottom", "0");

    Checkbox consentBox = new Checkbox(getTranslation("register.consent.label"));

    // Privacy policy link beneath the checkbox
    Paragraph privacyRef = new Paragraph();
    privacyRef.add(new Text(getTranslation("register.privacy.ref.before")));
    RouterLink privacyLink =
        new RouterLink(getTranslation("register.privacy.ref.link"), DatenschutzView.class);
    privacyLink.getElement().setAttribute("target", "_blank");
    privacyRef.add(privacyLink);
    privacyRef.add(new Text(getTranslation("register.privacy.ref.after")));
    privacyRef
        .getStyle()
        .set("font-size", "var(--lumo-font-size-s)")
        .set("color", "var(--lumo-secondary-text-color)")
        .set("margin-top", "var(--lumo-space-xs)");

    // ── Submit ────────────────────────────────────────────────────────────────
    Button submit = new Button(getTranslation("register.action.submit"));
    submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
    submit.setWidthFull();
    submit.addClickListener(
        e -> handleSubmit(binder, dto, loginName, password, honeypot, consentBox));

    // ── Footer link ───────────────────────────────────────────────────────────
    Span loginPrompt = new Span(getTranslation("register.footer.alreadyHaveAccount") + " ");
    RouterLink loginLink = new RouterLink(getTranslation("register.footer.login"), LoginView.class);
    HorizontalLayout footer = new HorizontalLayout(loginPrompt, loginLink);
    footer.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    footer.getStyle().set("font-size", "var(--lumo-font-size-s)");

    card.add(
        header,
        loginName,
        displayName,
        password,
        passwordConfirm,
        honeypot,
        divider,
        contactExplainerBox,
        divider2,
        privacyRef,
        consentNotice,
        consentBox,
        submit,
        footer);

    return card;
  }

  /**
   * Styled info box explaining why contact details are needed and that they will be shared. Placed
   * between the account fields and the consent checkbox so users see it before agreeing.
   */
  private Component buildContactExplainerBox() {
    Div box = new Div();
    box.setWidthFull();
    box.setMaxWidth(MainLayout.MAX_DISPLAYPORT_WIDTH);
    box.getStyle()
        .set("background-color", "var(--lumo-primary-color-10pct)")
        .set("border-left", "4px solid var(--lumo-primary-color)")
        .set("border-radius", "var(--lumo-border-radius-m)")
        .set("padding", "var(--lumo-space-m)")
        .set("box-sizing", "border-box");

    Span icon = new Span(VaadinIcon.INFO_CIRCLE.create());
    H3 sectionTitle = new H3(getTranslation("register.contactinfo.section.title"));
    sectionTitle
        .getStyle()
        .set("margin", "0 0 var(--lumo-space-xs) 0")
        .set("font-size", "var(--lumo-font-size-m)");
    HorizontalLayout horizontalLayout = new HorizontalLayout(icon, sectionTitle);
    Paragraph explainer = new Paragraph(getTranslation("register.contactinfo.explainer"));
    explainer.getStyle().set("margin", "0").set("font-size", "var(--lumo-font-size-s)");
    box.add(horizontalLayout, explainer);
    return box;
  }

  /**
   * Validates and submits the registration form.
   *
   * <p>Includes two silent bot-rejection checks before normal validation:
   *
   * <ol>
   *   <li>Honeypot field must be empty (bots fill all fields).
   *   <li>At least 3 seconds must have passed since the form was rendered (bots submit instantly).
   * </ol>
   */
  private void handleSubmit(
      Binder<RegisteredUser.Creation.CreationBuilder> binder,
      RegisteredUser.Creation.CreationBuilder dto,
      TextField loginName,
      PasswordField password,
      TextField honeypot,
      Checkbox consentBox) {

    // Bot check 1: honeypot
    if (!honeypot.getValue().isBlank()) {
      log.info("Honeypot register trap triggered: {}", honeypot.getValue());
      return; // silent reject — real users never see or fill this field
    }

    // Bot check 2: submission speed
    if (Duration.between(formRenderedAt, Instant.now()).getSeconds() < 3) {
      return; // silent reject — real users take longer than 3 seconds
    }

    // Consent is mandatory
    if (!consentBox.getValue()) {
      Notification n =
          Notification.show(
              getTranslation("register.error.consent.required"),
              4000,
              Notification.Position.MIDDLE);
      n.addThemeVariants(NotificationVariant.LUMO_ERROR);
      return;
    }

    if (!binder.writeBeanIfValid(dto)) {
      return;
    }

    dto.preferredLocale(getLocale());
    RegisteredUser registeredUser = userWorkflows.create(dto.build());

    // Auto-login so the user lands directly on the contact settings page
    try {
      autoLoginService.loginAfterRegister(registeredUser, password.getValue());
      /*
            HttpServletRequest request = VaadinServletRequest.getCurrent().getHttpServletRequest();
            request
                .getSession(true)
                .setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());
      */
      UI.getCurrent().navigate(ContactSettingsView.class);

    } catch (Exception ex) {
      // Auto-login failed (shouldn't happen right after creation) — fall back to login page
      UI.getCurrent().navigate(LoginView.class);
    }
  }
}

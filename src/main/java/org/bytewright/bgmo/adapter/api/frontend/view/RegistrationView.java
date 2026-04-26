package org.bytewright.bgmo.adapter.api.frontend.view;

import static org.bytewright.bgmo.domain.service.CoreAppContextConfig.APP_NAME_SHORT;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.Locale;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.domain.service.security.PasswordRules;
import org.bytewright.bgmo.usecases.UserWorkflows;

@Route("register")
@PageTitle("Join " + APP_NAME_SHORT)
@AnonymousAllowed
public class RegistrationView extends VerticalLayout {

  private final UserWorkflows userWorkflows;

  public RegistrationView(UserWorkflows userWorkflows) {
    this.userWorkflows = userWorkflows;

    addClassName("registration-view");
    setAlignItems(Alignment.CENTER);
    // setJustifyContentMode(JustifyContentMode.CENTER);

    setSizeFull();
    setPadding(true);
    setSpacing(true);
    getStyle().set("max-width", MainLayout.MAX_DISPLAYPORT_WIDTH).set("margin", "0 auto");
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

    ComboBox<Locale> localePicker = new ComboBox<>();
    localePicker.setItems(Locale.GERMAN, Locale.ENGLISH);
    localePicker.setValue(getLocale().getLanguage().equals("de") ? Locale.GERMAN : Locale.ENGLISH);
    localePicker.setItemLabelGenerator(l -> l.equals(Locale.GERMAN) ? "🇩🇪 DE" : "🇬🇧 EN");
    localePicker.setWidth("100px");
    localePicker.addValueChangeListener(e -> UI.getCurrent().getSession().setLocale(e.getValue()));

    header.add(title, localePicker);

    // --- Account Fields ---
    Binder<RegisteredUser.Creation> binder = new Binder<>();
    RegisteredUser.Creation.CreationBuilder dto = RegisteredUser.Creation.builder();

    TextField loginName = new TextField(getTranslation("register.field.loginName"));
    loginName.setWidthFull();
    loginName.setRequired(true);
    binder
        .forField(loginName)
        .asRequired(getTranslation("register.error.required"))
        .withValidator(v -> v.length() >= 3, getTranslation("register.error.name.tooShort"))
        .withValidator(
            userWorkflows::validateLoginName,
            getTranslation("register.error.loginName.alreadyTaken"))
        .bind(c -> "", (c, v) -> dto.loginName(v));

    TextField displayName = new TextField(getTranslation("register.field.displayName"));
    displayName.setWidthFull();
    displayName.setRequired(true);
    binder
        .forField(displayName)
        .asRequired(getTranslation("register.error.required"))
        .withValidator(v -> v.length() >= 3, getTranslation("register.error.name.tooShort"))
        .withValidator(
            userWorkflows::validateDisplayName,
            getTranslation("register.error.displayName.invalid")) // profanity filter
        .bind(c -> "", (c, v) -> dto.displayName(v));

    PasswordField password = new PasswordField(getTranslation("register.field.password"));
    password.setWidthFull();
    password.setRequired(true);
    binder
        .forField(password)
        .asRequired(getTranslation("register.error.required"))
        .withValidator(
            v -> v.length() >= PasswordRules.PW_MIN_CHARS,
            getTranslation("register.error.password.tooShort"))
        .bind(c -> "", (c, v) -> dto.password(v));

    PasswordField passwordConfirm =
        new PasswordField(getTranslation("register.field.passwordConfirm"));
    passwordConfirm.setWidthFull();
    passwordConfirm.setRequired(true);
    binder
        .forField(passwordConfirm)
        .withValidator(
            v -> v.equals(password.getValue()), getTranslation("register.error.password.mismatch"))
        .bind(c -> "", (c, v) -> {});

    // --- Divider + intro section ---
    Hr divider = new Hr();

    Paragraph introHint = new Paragraph(getTranslation("register.intro.hint"));
    introHint
        .getStyle()
        .set("color", "var(--lumo-secondary-text-color)")
        .set("font-size", "var(--lumo-font-size-s)")
        .set("margin-top", "0");

    TextArea aboutYourself =
        buildIntroArea(
            getTranslation("register.intro.aboutYourself"),
            getTranslation("register.intro.aboutYourself.hint"));
    binder.forField(aboutYourself).bind(c -> "", (c, v) -> dto.introAboutYourself(v));

    TextArea howDidYouHear =
        buildIntroArea(
            getTranslation("register.intro.howDidYouHear"),
            getTranslation("register.intro.howDidYouHear.hint"));
    binder.forField(howDidYouHear).bind(c -> "", (c, v) -> dto.introHowDidYouHear(v));

    TextArea whoInvitedYou =
        buildIntroArea(
            getTranslation("register.intro.whoInvitedYou"),
            getTranslation("register.intro.whoInvitedYou.hint"));
    binder.forField(whoInvitedYou).bind(c -> "", (c, v) -> dto.introWhoInvitedYou(v));

    // --- Submit ---
    Button submit = new Button(getTranslation("register.action.submit"));
    submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
    submit.setWidthFull();
    submit.addClickListener(
        e -> {
          if (binder.validate().isOk()) {
            dto.preferredLocale(localePicker.getValue());
            userWorkflows.create(dto.build());
            Notification notification =
                Notification.show(getTranslation("register.status.submitted"));
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            UI.getCurrent().navigate(LoginView.class);
          }
        });

    // --- Footer link ---
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
        divider,
        introHint,
        aboutYourself,
        howDidYouHear,
        whoInvitedYou,
        submit,
        footer);

    return card;
  }

  private TextArea buildIntroArea(String label, String placeholder) {
    TextArea area = new TextArea(label);
    area.setPlaceholder(placeholder);
    area.setWidthFull();
    area.setMinHeight("80px");
    area.setMaxHeight("160px");
    return area;
  }
}

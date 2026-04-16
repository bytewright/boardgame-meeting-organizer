package org.bytewright.bgmo.adapter.api.frontend.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.bytewright.bgmo.domain.model.user.RegisteredUser;
import org.bytewright.bgmo.usecases.UserWorkflows;
import org.springframework.util.StringUtils;

@Route("register")
@PageTitle("Join BGMO")
@AnonymousAllowed
public class RegistrationView extends VerticalLayout {

  private final UserWorkflows userWorkflows;
  private final VerticalLayout content;

  // Data Holders (to be sent to Workflow at the end)
  private final RegisteredUser.Creation.CreationBuilder userDto = RegisteredUser.Creation.builder();
  private ContactInfo primaryContact;
  private ContactInfo.AddressContact.AddressContactBuilder hostingAddress =
      ContactInfo.AddressContact.builder();

  public RegistrationView(UserWorkflows userWorkflows) {
    this.userWorkflows = userWorkflows;

    addClassName("registration-view");
    setAlignItems(Alignment.CENTER);
    setJustifyContentMode(JustifyContentMode.CENTER);
    setSizeFull();

    content = new VerticalLayout();
    content.setMaxWidth("450px"); // Mobile-first width
    content.setPadding(true);
    content.getStyle().set("box-shadow", "var(--lumo-box-shadow-m)");
    content.getStyle().set("border-radius", "var(--lumo-border-radius-l)");

    add(new H1("Create Account"), content);
    showStep(1);
  }

  private void showStep(int step) {
    content.removeAll();
    switch (step) {
      case 1 -> createAccountStep();
      case 2 -> contactPreferenceStep();
      case 3 -> hostingStep();
    }
  }

  private void createAccountStep() {
    TextField login = new TextField("Username");
    TextField display = new TextField("Display Name");
    PasswordField pwd = new PasswordField("Password");

    Button next =
        new Button(
            "Next",
            e -> {
              userDto
                  .loginName(login.getValue())
                  .displayName(display.getValue())
                  .password(pwd.getValue());
              showStep(2);
            });
    next.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    next.setWidthFull();

    content.add(login, display, pwd, next);
  }

  private void contactPreferenceStep() {
    Span info = new Span("How should we notify you about meetups?");
    RadioButtonGroup<ContactInfoType> typePicker = new RadioButtonGroup<>("Primary Channel");
    typePicker.setItems(ContactInfoType.EMAIL, ContactInfoType.TELEGRAM, ContactInfoType.SIGNAL);

    TextField handleField = new TextField("Handle / Address");
    handleField.setVisible(false);

    typePicker.addValueChangeListener(
        e -> {
          handleField.setVisible(true);
          handleField.setLabel(e.getValue().name() + " Details");
        });

    Button next =
        new Button(
            "Almost Done",
            e -> {
              // Logic to build the specific record type
              String val = handleField.getValue();
              if (typePicker.getValue() == ContactInfoType.EMAIL) userDto.email(val);
              if (typePicker.getValue() == ContactInfoType.TELEGRAM) userDto.telegramHandle(val);
              if (typePicker.getValue() == ContactInfoType.SIGNAL) userDto.signalHandle(val);
              showStep(3);
            });
    next.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    next.setWidthFull();

    content.add(info, typePicker, handleField, next);
  }

  private void hostingStep() {
    Span info = new Span("Optional: Do you plan on hosting game nights?");
    info.getStyle().set("font-size", "var(--lumo-font-size-s)");

    Details addressDetails = new Details("Add Hosting Address", createAddressForm());

    Button finish =
        new Button(
            "Submit Registration",
            e -> {
              RegisteredUser user = userWorkflows.create(userDto.build());
              var address = hostingAddress.build();
              if (StringUtils.hasText(address.city())
                  && StringUtils.hasText(address.street())
                  && StringUtils.hasText(address.zipCode())) {
                userWorkflows.addContactInfo(user.getId(), address);
              }
              Notification.show("Registration submitted! An admin will approve you shortly.");
              UI.getCurrent().navigate(LoginView.class);
            });
    finish.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
    finish.setWidthFull();

    content.add(info, addressDetails, finish);
  }

  private VerticalLayout createAddressForm() {
    TextField street =
        new TextField("Street & Nr", "", event -> hostingAddress.street(event.getValue()));
    TextField zip = new TextField("Zip", "", event -> hostingAddress.zipCode(event.getValue()));
    TextField city = new TextField("City", "", event -> hostingAddress.city(event.getValue()));
    return new VerticalLayout(street, zip, city);
  }
}

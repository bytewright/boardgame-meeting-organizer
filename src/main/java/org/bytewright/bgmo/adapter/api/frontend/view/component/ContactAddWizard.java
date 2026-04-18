package org.bytewright.bgmo.adapter.api.frontend.view.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import java.util.function.Consumer;
import org.bytewright.bgmo.domain.model.user.ContactInfo;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;

public class ContactAddWizard extends Dialog {
  private final VerticalLayout stepContainer;
  private final Consumer<ContactInfo> onComplete;

  public ContactAddWizard(Consumer<ContactInfo> onComplete) {
    this.onComplete = onComplete;
    setHeaderTitle("Add Contact Information");

    stepContainer = new VerticalLayout();
    add(stepContainer);

    showStep1();

    getFooter().add(new Button("Cancel", e -> close()));
  }

  private void showStep1() {
    stepContainer.removeAll();
    ComboBox<ContactInfoType> typePicker = new ComboBox<>("Select Type", ContactInfoType.values());
    typePicker.setWidthFull();

    Button nextBtn =
        new Button(
            "Next",
            e -> {
              if (typePicker.getValue() != null) showStep2(typePicker.getValue());
            });
    nextBtn.setEnabled(false);
    typePicker.addValueChangeListener(e -> nextBtn.setEnabled(e.getValue() != null));

    stepContainer.add(typePicker, nextBtn);
  }

  private void showStep2(ContactInfoType type) {
    stepContainer.removeAll();
    TextField valueField = new TextField("Enter " + type.name());
    valueField.setWidthFull();

    Button finishBtn =
        new Button(
            "Finish",
            e -> {
              ContactInfo newContactInfo =
                  switch (type) {
                    case EMAIL -> {
                      yield ContactInfo.EmailContact.builder().email(valueField.getValue()).build();
                    }
                    case TELEGRAM -> {
                      yield ContactInfo.TelegramContact.builder()
                          .chatId(valueField.getValue())
                          .build();
                    }
                    case SIGNAL -> {
                      yield ContactInfo.SignalContact.builder()
                          .signalHandle(valueField.getValue())
                          .build();
                    }
                    case ADDRESS -> {
                      // TODO this makes no sense, the wizard needs much more logic
                      yield ContactInfo.AddressContact.builder()
                          .street(valueField.getValue())
                          .build();
                    }
                    case PHONE -> {
                      yield ContactInfo.PhoneContact.builder()
                          .phoneNr(valueField.getValue())
                          .build();
                    }
                  };
              onComplete.accept(newContactInfo);
              close();
            });

    stepContainer.add(valueField, finishBtn);
  }
}

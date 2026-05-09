package org.bytewright.bgmo.adapter.api.frontend.view.component;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.combobox.ComboBox;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.api.frontend.service.i18n.LocaleService;

@Slf4j
public class LocalePicker extends ComboBox<Locale> {

  public LocalePicker(LocaleService localeService) {
    List<Locale> supportedLocales = localeService.getSupportedLocales();
    setItems(supportedLocales);
    setValue(getLocale());
    setItemLabelGenerator(localeService::getLabel);
    setWidth(80, Unit.PIXELS);
    addValueChangeListener(
        e -> {
          localeService.changeLocale(e.getValue());
        });
  }
}

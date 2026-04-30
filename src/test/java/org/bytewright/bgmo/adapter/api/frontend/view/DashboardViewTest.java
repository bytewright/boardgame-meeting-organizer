package org.bytewright.bgmo.adapter.api.frontend.view;

import static org.junit.jupiter.api.Assertions.*;

import com.vaadin.browserless.SpringBrowserlessTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DashboardViewTest extends SpringBrowserlessTest {
  @Test
  void test() {
    DashboardView view = navigate(DashboardView.class);

    test(view).ensureComponentIsUsable();
  }
}

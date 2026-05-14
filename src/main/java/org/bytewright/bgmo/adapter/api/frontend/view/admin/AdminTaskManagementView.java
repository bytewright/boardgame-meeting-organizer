package org.bytewright.bgmo.adapter.api.frontend.view.admin;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.adapter.api.frontend.view.component.MainLayout;
import org.bytewright.bgmo.domain.model.automation.ScheduledTask;
import org.bytewright.bgmo.domain.service.data.AutomationTaskDao;
import org.springframework.data.domain.Sort;

@Slf4j
@Route(value = "admin/tasks", layout = MainLayout.class)
@PageTitle("Admin | Task Management")
@RolesAllowed("ADMIN")
public class AdminTaskManagementView extends VerticalLayout {

  private final AutomationTaskDao taskDao;
  private final Grid<ScheduledTask> grid = new Grid<>(ScheduledTask.class, false);
  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  public AdminTaskManagementView(AutomationTaskDao taskDao) {
    this.taskDao = taskDao;
    setSizeFull();

    add(new H2("System Automation Tasks"));

    configureGrid();
    add(grid);
  }

  private void configureGrid() {
    grid.setSizeFull();
    grid.addColumn(ScheduledTask::getTaskState).setHeader("Status").setFlexGrow(0);
    grid.addColumn(ScheduledTask::getTsDueDate).setHeader("Due Date").setSortable(true);
    grid.addColumn(task -> task.getPayload().discriminator()).setHeader("Type");
    grid.addColumn(task -> task.getId().toString().substring(0, 5) + "...").setHeader("Task ID");

    grid.addComponentColumn(
            task -> {
              Span badge = new Span(task.getTaskState().name());
              String theme =
                  switch (task.getTaskState()) {
                    case FINISHED -> "badge success";
                    case ERROR -> "badge error";
                    case EXECUTING -> "badge contrast";
                    default -> "badge";
                  };
              badge.getElement().getThemeList().add(theme);
              return badge;
            })
        .setHeader("Status Indicator");
  }

  private void refreshGrid(UI ui) {
    AutomationTaskDao.ScheduledTaskPage page =
        taskDao.findAllTasks(0, 50, AutomationTaskDao.TaskSorting.DUE_DATE, Sort.Direction.DESC);

    List<ScheduledTask> tasks = page.content();

    ui.access(() -> grid.setItems(tasks));
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    UI ui = attachEvent.getUI();
    // Initial load
    refreshGrid(ui);

    // Schedule periodic updates every 5 seconds
    executor.scheduleAtFixedRate(() -> refreshGrid(ui), 5, 5, TimeUnit.SECONDS);
  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    // Shutdown the executor when the user leaves the view to prevent memory leaks
    executor.shutdown();
    try {
      if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
    }
  }
}

package org.bytewright.bgmo.domain.service.automation;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

@EnableAsync
@Configuration
public class QuartzSchedulerContextConfig {

  /** Creates a custom JobFactory that integrates with Spring */
  @Bean
  public SpringBeanJobFactory springBeanJobFactory(ApplicationContext applicationContext) {
    SpringBeanJobFactory jobFactory = new SpringBeanJobFactory();
    jobFactory.setApplicationContext(applicationContext);
    return jobFactory;
  }

  /** Creates the SchedulerFactoryBean that will manage the Quartz scheduler */
  @Bean
  public SchedulerFactoryBean schedulerFactoryBean(
      @Value("${org.bytewright.bgmo.automation-autostart:true}") boolean isAutostart,
      @Autowired(required = false) DataSource dataSource,
      ApplicationContext applicationContext,
      SpringBeanJobFactory springBeanJobFactory) {
    SchedulerFactoryBean schedulerFactory = new SchedulerFactoryBean();

    // Set the JobFactory to integrate with Spring
    schedulerFactory.setJobFactory(springBeanJobFactory);
    schedulerFactory.setApplicationContext(applicationContext);

    // Use the DataSource if provided, tbd if persistent jobs are needed
    if (false && dataSource != null) {
      schedulerFactory.setDataSource(dataSource);
    }
    schedulerFactory.setWaitForJobsToCompleteOnShutdown(true);
    schedulerFactory.setAutoStartup(isAutostart);
    schedulerFactory.setSchedulerName("App-Quartz-Scheduler");
    schedulerFactory.setStartupDelay(10);
    return schedulerFactory;
  }
}

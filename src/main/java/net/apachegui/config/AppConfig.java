package net.apachegui.config;

import jakarta.servlet.ServletContextListener;
import org.springframework.beans.factory.annotation.Qualifier;
import net.apachegui.global.SearchTaskExecutor;
import net.apachegui.global.ServerContextListener;
import net.apachegui.history.HistoryMaintenance;
import net.apachegui.modules.SharedModuleFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Application beans migrated from the former {@code spring-servlet.xml} and {@code web.xml}:
 * the search task executor, the hourly history-maintenance task, and the servlet
 * listener/filter that were previously declared in web.xml.
 */
@Configuration
public class AppConfig {

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }

    @Bean
    public SearchTaskExecutor search(@Qualifier("taskExecutor") TaskExecutor taskExecutor) {
        return new SearchTaskExecutor(taskExecutor);
    }

    @Bean
    public HistoryMaintenance historyMaintenance() {
        return new HistoryMaintenance();
    }

    /** Replaces the spring-task {@code scheduled-tasks} entry: clean history every hour. */
    @Scheduled(fixedRate = 3600000)
    public void cleanHistory() throws Exception {
        historyMaintenance().clean();
    }

    @Bean
    public ServletListenerRegistrationBean<ServletContextListener> serverContextListener() {
        return new ServletListenerRegistrationBean<>(new ServerContextListener());
    }

    @Bean
    public FilterRegistrationBean<SharedModuleFilter> sharedModuleFilter() {
        FilterRegistrationBean<SharedModuleFilter> registration = new FilterRegistrationBean<>(new SharedModuleFilter());
        registration.addUrlPatterns("/web/Init/CheckFirstTime");
        return registration;
    }
}

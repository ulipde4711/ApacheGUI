package net.apachegui.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves the static webapp directories that were previously declared as
 * {@code <mvc:resources>} entries in spring-servlet.xml. Locations are relative
 * to the servlet context root (the packaged webapp).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/resources/**").addResourceLocations("/resources/");
        registry.addResourceHandler("/HistoryFiles/**").addResourceLocations("/HistoryFiles/");
        registry.addResourceHandler("/search/**").addResourceLocations("/search/");
        registry.addResourceHandler("/manual/**").addResourceLocations("/manual/");
    }
}

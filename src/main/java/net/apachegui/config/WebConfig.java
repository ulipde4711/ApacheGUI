package net.apachegui.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
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

    /**
     * The dojo/dojox front end (e.g. the JsonRestStore backing the navigation
     * tree) requests some endpoints with a trailing slash — "/web/Menu/rest/".
     * Spring MVC matched that against the "/web/Menu/rest" mapping until Spring 6
     * disabled trailing-slash matching by default, which now misroutes such calls
     * to the "/rest/**" child handler and returns an empty body. Re-enable the
     * legacy behaviour app-wide. Deprecated upstream; revisit when the Dojo front
     * end is replaced.
     */
    @Override
    @SuppressWarnings("deprecation")
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseTrailingSlashMatch(true);
    }
}

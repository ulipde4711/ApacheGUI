package net.apachegui.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Restores the JSP servlet mapping that the former web.xml used.
 *
 * <p>The embedded Tomcat registers Jasper on {@code *.jsp} by default, which would
 * intercept the application's public {@code /jsp/*.jsp} view URLs (handled by
 * {@code GUIViewController}) before they reach the DispatcherServlet. The original
 * web.xml mapped the JSP servlet to {@code /WEB-INF/jsp/*} only, so Jasper handled
 * just the internal forwards. We reproduce that here.
 */
@Configuration
public class WebServerConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> jspServletMappingCustomizer() {
        return factory -> factory.addContextCustomizers(context -> {
            context.removeServletMapping("*.jsp");
            context.addServletMappingDecoded("/WEB-INF/jsp/*", "jsp");
        });
    }
}

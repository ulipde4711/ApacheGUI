package net.apachegui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot entry point. Replaces the former web.xml / spring-servlet.xml bootstrap.
 *
 * <p>Built as an executable WAR: it can be launched directly with
 * {@code java -jar ApacheGUI.war} (embedded Tomcat) and still deploys to an
 * external servlet container.
 */
@SpringBootApplication
@EnableScheduling
public class ApacheGuiApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(ApacheGuiApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(ApacheGuiApplication.class, args);
    }
}

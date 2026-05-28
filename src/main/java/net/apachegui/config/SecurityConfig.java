package net.apachegui.config;

import jakarta.servlet.DispatcherType;
import net.apachegui.db.UsersDao;
import net.apachegui.db.SettingsDao;
import net.apachegui.global.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Replaces the container-managed FORM authentication (the former
 * {@code net.apachegui.tomcat.ApacheGUIRealm} + web.xml security-constraint).
 *
 * <p>Authenticates the single admin account stored in {@link UsersDao} (SQLite).
 * Passwords are BCrypt-hashed; an existing pre-migration credential must be reset
 * (re-run the first-time init flow) before login will succeed.
 *
 * <p>The login form ({@code /WEB-INF/jsp/views/Login.jsp}) keeps its legacy
 * {@code j_security_check} / {@code j_username} / {@code j_password} conventions.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            UsersDao users = UsersDao.getInstance();
            String storedUser = users.getUsername();
            String storedPass = users.getPassword();
            if (storedUser == null || storedPass == null || !storedUser.equals(username)) {
                throw new UsernameNotFoundException("Unknown user: " + username);
            }
            return User.withUsername(storedUser)
                    .password(storedPass)
                    .roles("apachegui")
                    .build();
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // The GUI's dojo-based AJAX POSTs do not carry CSRF tokens; the prior
            // container security had none either. Tracked as security debt.
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Internal JSP view forwards/includes must not be re-authenticated
                // (Spring Security 6 filters all dispatcher types by default).
                .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.INCLUDE).permitAll()
                .requestMatchers("/jsp/Login.jsp", "/j_security_check",
                        "/resources/**", "/manual/**", "/error").permitAll()
                // Everything else is open until first-time setup completes (so the
                // wizard can run and create the first account), then locked down.
                // This also keeps /web/Init/** authenticated post-setup, preventing
                // a re-run of the wizard (which wipes the databases) by an anonymous
                // caller.
                .anyRequest().access(notInitializedOrAuthenticated()))
            .formLogin(form -> form
                .loginPage("/jsp/Login.jsp")
                .loginProcessingUrl("/j_security_check")
                .usernameParameter("j_username")
                .passwordParameter("j_password")
                .defaultSuccessUrl("/", true)
                .failureUrl("/jsp/Login.jsp?error=true")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/jsp/Login.jsp"));
        return http.build();
    }

    /**
     * Grants access while the application has not yet been initialized (the
     * {@link Constants#INIT} setting is absent), so the first-run setup wizard —
     * which runs on the authenticated main page and creates the first account —
     * is reachable without a login. Once setup completes, normal authentication
     * is required.
     */
    private AuthorizationManager<RequestAuthorizationContext> notInitializedOrAuthenticated() {
        return (authentication, context) -> {
            if (SettingsDao.getInstance().getSetting(Constants.INIT) == null) {
                return new AuthorizationDecision(true);
            }
            Authentication auth = authentication.get();
            boolean authenticated = auth != null && auth.isAuthenticated()
                    && !(auth instanceof AnonymousAuthenticationToken);
            return new AuthorizationDecision(authenticated);
        };
    }
}

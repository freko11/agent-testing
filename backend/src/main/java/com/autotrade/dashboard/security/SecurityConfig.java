package com.autotrade.dashboard.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Single-operator dashboard login (E1-F3-S1). No user table — this is a
 * personal, single-user tool, so one credential sourced from env vars is
 * enough; session cookie + CSRF cookie is the whole auth mechanism, no
 * JWT/OAuth complexity.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private static final String DEV_FALLBACK_PASSWORD = "insecure-dev-only-password-do-not-use-in-paper-or-prod";

    @Value("${dashboard.auth.username:admin}")
    private String username;

    @Value("${dashboard.auth.password:}")
    private String password;

    @Value("${dashboard.auth.password-hash:}")
    private String passwordHash;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        String hash;
        if (passwordHash != null && !passwordHash.isBlank()) {
            hash = passwordHash;
        } else if (password != null && !password.isBlank()) {
            log.warn("DASHBOARD_PASSWORD_HASH is not set; hashing DASHBOARD_PASSWORD at startup instead. "
                    + "Set DASHBOARD_PASSWORD_HASH in paper/prod.");
            hash = passwordEncoder.encode(password);
        } else {
            log.warn("Neither DASHBOARD_PASSWORD nor DASHBOARD_PASSWORD_HASH is set; using an insecure "
                    + "dev-only fallback password. Set DASHBOARD_PASSWORD_HASH in paper/prod.");
            hash = passwordEncoder.encode(DEV_FALLBACK_PASSWORD);
        }
        UserDetails operator = User.withUsername(username).password(hash).roles("OPERATOR").build();
        return new InMemoryUserDetailsManager(operator);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .addFilterAfter(new CsrfCookieWriteFilter(), CsrfFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        // "/health" is the real path in every profile (management.endpoints.web.base-path=/
                        // in application.properties); "/actuator/health" is also allowed since the test
                        // classpath's application.properties doesn't inherit that base-path override.
                        .requestMatchers("/health", "/actuator/health", "/api/auth/csrf", "/api/auth/login")
                        .permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_OK))
                        .failureHandler((request, response, exception) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials")))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_OK)))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required")));

        return http.build();
    }

    /**
     * CookieCsrfTokenRepository only writes the XSRF-TOKEN cookie once the
     * deferred {@link CsrfToken} is actually read — which nothing does by
     * default in a pure REST API (no view ever renders {@code _csrf}). This
     * forces that read on every request so the SPA always has a fresh
     * cookie to echo back as X-XSRF-TOKEN on the next state-changing call.
     */
    /**
     * The default {@link XorCsrfTokenRequestAttributeHandler} XOR-masks the
     * token when exposing it for server-rendered HTML forms (BREACH
     * mitigation) — but that means it also expects submitted tokens to be
     * masked, which breaks a plain cookie-reading SPA that just echoes the
     * raw cookie value back in a header. This resolves straight from the
     * header when present (matching the raw, unmasked cookie value) and
     * only falls back to the XOR-aware parameter resolution otherwise —
     * the pattern Spring Security's own reference docs recommend for the
     * cookie-based SPA case.
     */
    private static final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {
        private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response,
                            Supplier<CsrfToken> csrfToken) {
            delegate.handle(request, response, csrfToken);
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            String headerValue = request.getHeader(csrfToken.getHeaderName());
            return StringUtils.hasText(headerValue) ? headerValue : super.resolveCsrfTokenValue(request, csrfToken);
        }
    }

    private static final class CsrfCookieWriteFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                         FilterChain filterChain) throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}

package com.possable.config;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${possable.security.api-key}")
    private String configuredApiKey;

    @Value("${possable.security.bearer-token-secret}")
    private String configuredBearerSecret;

    @Value("${app.auth.pincode:1234}")
    private String defaultPincode;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            // Allow session for simple PIN-based login flows
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .addFilterBefore(authenticationFilter(), AbstractPreAuthenticatedProcessingFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/index.html", "/favicon.ico", "/sw.js", "/manifest.json",
                    "/frontend/**", "/frontend-es5/**", "/frontend-es6/**", "/VAADIN/**",
                    "/icons/**", "/images/**", "/styles/**", "/themes/**",
                    "/health", "/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/docs",
                    "/entry", "/pincode-login"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.disable())
            .exceptionHandling(eh -> eh.authenticationEntryPoint((req, res, authEx) -> {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.setContentType("application/json");
                res.getWriter().write("{\"error\":\"unauthorized\"}");
            }));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService users(PasswordEncoder encoder) {
        // simple in-memory users for PIN authentication; PINs are encoded with BCrypt
        String encodedPin = encoder.encode(defaultPincode);
        var service = User.withUsername("service").password(encodedPin).roles("SERVICE").build();
        var kitchen = User.withUsername("kitchen").password(encodedPin).roles("KITCHEN").build();
        var management = User.withUsername("management").password(encodedPin).roles("MANAGEMENT").build();
        return new InMemoryUserDetailsManager(service, kitchen, management);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public OncePerRequestFilter authenticationFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
                try {
                    String path = request.getRequestURI();
                    String method = request.getMethod();

                    // Allow public/static paths and preflight requests without authentication
                    String accept = request.getHeader("Accept");
                    boolean isBrowserNavigation = "GET".equalsIgnoreCase(method) && accept != null && accept.contains("text/html");

                    if ("OPTIONS".equalsIgnoreCase(method)
                        || isBrowserNavigation // allow normal browser navigation (Vaadin views) for GET
                        || path.equals("/index.html") || path.equals("/favicon.ico")
                        || path.equals("/sw.js") || path.equals("/manifest.json")
                        || path.startsWith("/frontend/") || path.startsWith("/frontend-es5/") || path.startsWith("/frontend-es6/")
                        || path.startsWith("/VAADIN/")
                        || path.startsWith("/icons/") || path.startsWith("/images/") || path.startsWith("/styles/") || path.startsWith("/themes/")
                        || path.equals("/health") || path.equals("/actuator/health")
                        || path.startsWith("/v3/api-docs/") || path.startsWith("/swagger-ui/") || path.equals("/swagger-ui.html")
                        || path.equals("/docs") || path.equals("/entry") || path.equals("/pincode-login")) {
                        filterChain.doFilter(request, response);
                        return;
                    }

                    String apiKey = request.getHeader("X-API-KEY");
                    String authHeader = request.getHeader("Authorization");

                    boolean valid = false;
                    String userIdForMdc = null;

                    if (apiKey != null && apiKey.equals(configuredApiKey)) {
                        valid = true;
                        userIdForMdc = "apiKey:***"; // mask actual key
                    }

                    if (!valid && authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        // Simple constant comparison for starter implementation; replace with JWT or token service later
                        if (configuredBearerSecret != null && !configuredBearerSecret.isEmpty() && configuredBearerSecret.equals(token)) {
                            valid = true;
                            userIdForMdc = "bearer:***"; // mask token
                        }
                    }

                    if (valid) {
                        // set a simple authentication so downstream code can rely on SecurityContext
                        var auth = new UsernamePasswordAuthenticationToken(userIdForMdc, null, List.of(new SimpleGrantedAuthority("ROLE_API")));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        MDC.put("user_id", userIdForMdc);
                    }

                    try {
                        filterChain.doFilter(request, response);
                    } finally {
                        if (userIdForMdc != null) {
                            MDC.remove("user_id");
                            SecurityContextHolder.clearContext();
                        }
                    }
                } catch (Exception ex) {
                    log.error("Authentication filter failure", ex);
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"internal_error\"}");
                }
            }
        };
    }
} 
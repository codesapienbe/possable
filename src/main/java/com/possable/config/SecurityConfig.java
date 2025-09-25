package com.possable.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${possable.security.api-key}")
    private String configuredApiKey;

    @Value("${possable.security.bearer-token-secret}")
    private String configuredBearerSecret;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(authenticationFilter(), AbstractPreAuthenticatedProcessingFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/index.html", "/favicon.ico", "/sw.js", "/manifest.json",
                    "/frontend/**", "/frontend-es5/**", "/frontend-es6/**", "/VAADIN/**",
                    "/icons/**", "/images/**", "/styles/**", "/themes/**",
                    "/health", "/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/docs"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
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
                    if ("OPTIONS".equalsIgnoreCase(method)
                        || path.equals("/") || path.equals("/index.html") || path.equals("/favicon.ico")
                        || path.equals("/sw.js") || path.equals("/manifest.json")
                        || path.startsWith("/frontend/") || path.startsWith("/frontend-es5/") || path.startsWith("/frontend-es6/")
                        || path.startsWith("/VAADIN/")
                        || path.startsWith("/icons/") || path.startsWith("/images/") || path.startsWith("/styles/") || path.startsWith("/themes/")
                        || path.equals("/health") || path.equals("/actuator/health")
                        || path.startsWith("/v3/api-docs/") || path.startsWith("/swagger-ui/") || path.equals("/swagger-ui.html")
                        || path.equals("/docs")) {
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

                    if (!valid) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"unauthorized\"}");
                        return;
                    }

                    if (userIdForMdc != null) {
                        MDC.put("user_id", userIdForMdc);
                    }

                    try {
                        filterChain.doFilter(request, response);
                    } finally {
                        if (userIdForMdc != null) MDC.remove("user_id");
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
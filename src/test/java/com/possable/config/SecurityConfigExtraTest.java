package com.possable.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityConfigExtraTest {

    private void setApiKey(SecurityConfig cfg, String apiKey) throws Exception {
        Field f1 = SecurityConfig.class.getDeclaredField("configuredApiKey");
        f1.setAccessible(true);
        f1.set(cfg, apiKey);
    }

    private void setBearer(SecurityConfig cfg, String bearer) throws Exception {
        Field f2 = SecurityConfig.class.getDeclaredField("configuredBearerSecret");
        f2.setAccessible(true);
        f2.set(cfg, bearer);
    }

    @Test
    public void noBearerConfigured_bearerHeaderRejected() throws Exception {
        SecurityConfig cfg = new SecurityConfig();
        setApiKey(cfg, "key123");
        setBearer(cfg, "");

        var filter = cfg.authenticationFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("Authorization", "Bearer some-token");

        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(req, res, chain);

        assertEquals(401, res.getStatus());
    }

    @Test
    public void bothHeadersPresent_apiKeyTakesPrecedence() throws Exception {
        SecurityConfig cfg = new SecurityConfig();
        setApiKey(cfg, "key123");
        setBearer(cfg, "bearer123");

        var filter = cfg.authenticationFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("X-API-KEY", "key123");
        req.addHeader("Authorization", "Bearer wrong");

        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(req, res, chain);

        assertEquals(200, res.getStatus());
    }
} 
package com.possable.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityConfigTest {

    private void setConfigKeys(SecurityConfig cfg) throws Exception {
        Field f1 = SecurityConfig.class.getDeclaredField("configuredApiKey");
        f1.setAccessible(true);
        f1.set(cfg, "changeme-api-key");
        Field f2 = SecurityConfig.class.getDeclaredField("configuredBearerSecret");
        f2.setAccessible(true);
        f2.set(cfg, "changeme-bearer-secret");
    }

    @Test
    public void authenticationFilter_allowsWithApiKey() throws Exception {
        SecurityConfig cfg = new SecurityConfig();
        setConfigKeys(cfg);
        var filter = cfg.authenticationFilter();

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("X-API-KEY", "changeme-api-key");

        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(req, res, chain);

        assertEquals(200, res.getStatus());
    }

    @Test
    public void authenticationFilter_rejectsWithoutCredentials() throws Exception {
        SecurityConfig cfg = new SecurityConfig();
        setConfigKeys(cfg);
        var filter = cfg.authenticationFilter();

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(req, res, chain);

        assertEquals(401, res.getStatus());
    }

    @Test
    public void authenticationFilter_allowsWithBearerToken() throws Exception {
        SecurityConfig cfg = new SecurityConfig();
        setConfigKeys(cfg);
        var filter = cfg.authenticationFilter();

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("Authorization", "Bearer changeme-bearer-secret");

        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(req, res, chain);

        assertEquals(200, res.getStatus());
    }
} 
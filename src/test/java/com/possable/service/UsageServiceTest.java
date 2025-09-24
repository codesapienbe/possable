package com.possable.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class UsageServiceTest {

    @Test
    public void incrementAndGet() {
        UsageService svc = new UsageService();
        int before = svc.getRequestsMade();
        svc.incrementRequests(3);
        assertEquals(before + 3, svc.getRequestsMade());
    }

    @Test
    public void resetWhenPast() throws Exception {
        UsageService svc = new UsageService();
        // set resetAt to past
        Field f = UsageService.class.getDeclaredField("resetAt");
        f.setAccessible(true);
        f.set(svc, Instant.parse("2000-01-01T00:00:00Z"));

        svc.incrementRequests(5);
        assertEquals(5, svc.getRequestsMade());
    }
} 
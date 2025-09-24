package com.possable.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class UsageService {

    private final int monthlyLimit = 100000; // default for skeleton
    private final AtomicInteger requestsMade = new AtomicInteger(0);
    private volatile Instant resetAt;

    public UsageService() {
        computeNextReset();
    }

    public synchronized void incrementRequests(int by) {
        if (by <= 0) return;
        // if past reset, reset counter
        if (resetAt.isBefore(Instant.now())) {
            requestsMade.set(0);
            computeNextReset();
        }
        requestsMade.addAndGet(by);
    }

    public int getMonthlyLimit() {
        return monthlyLimit;
    }

    public int getRequestsMade() {
        return requestsMade.get();
    }

    public Instant getResetAt() {
        return resetAt;
    }

    private void computeNextReset() {
        var now = LocalDate.now(ZoneOffset.UTC);
        var firstOfNextMonth = now.with(TemporalAdjusters.firstDayOfNextMonth()).atStartOfDay(ZoneOffset.UTC).toInstant();
        this.resetAt = firstOfNextMonth;
    }
} 
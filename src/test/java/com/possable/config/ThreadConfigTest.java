package com.possable.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ThreadConfigTest {

    @Test
    public void beansProvided() {
        ThreadConfig cfg = new ThreadConfig();
        ExecutorService es = cfg.virtualThreadExecutorService();
        assertNotNull(es);
        TaskExecutor te = cfg.virtualThreadTaskExecutor(es);
        assertNotNull(te);

        // ensure executor runs tasks
        CountDownLatch latch = new CountDownLatch(1);
        te.execute(latch::countDown);
        try {
            assertTrue(latch.await(1, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Interrupted while waiting for task to run");
        }
    }
} 
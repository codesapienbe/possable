package com.possable.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import java.util.concurrent.ExecutorService;

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
        final boolean[] ran = {false};
        te.execute(() -> ran[0] = true);
        // small sleep to allow virtual thread to run
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        assertTrue(ran[0]);
    }
} 
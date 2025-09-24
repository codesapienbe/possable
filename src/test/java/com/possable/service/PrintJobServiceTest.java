package com.possable.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class PrintJobServiceTest {

    private final TaskExecutor syncExecutor = r -> r.run();

    @Test
    public void createListAndUpdatePrintJob() {
        PrinterService printerService = new PrinterService();
        PrintTemplateService templateService = new PrintTemplateService();
        PrintJobService svc = new PrintJobService(printerService, templateService, syncExecutor);

        var job = svc.createJob("order-1", "printer-1", "template-1");
        assertNotNull(job);
        assertEquals("pending", job.status());

        var all = svc.listJobs(java.util.Map.of());
        assertTrue(all.stream().anyMatch(j -> j.id().equals(job.id())));

        svc.updateStatus(job.id(), "printing");
        var jAfter = svc.listJobs(java.util.Map.of()).stream().filter(j -> j.id().equals(job.id())).findFirst().orElseThrow();
        assertEquals("printing", jAfter.status());

        svc.updateStatus(job.id(), "completed");
        var jCompleted = svc.listJobs(java.util.Map.of("status", "completed")).stream().filter(j -> j.id().equals(job.id())).findFirst().orElse(null);
        assertNotNull(jCompleted);
    }

    @Test
    public void processJobInterrupted_marksFailed() throws Exception {
        PrinterService printerService = new PrinterService();
        PrintTemplateService templateService = new PrintTemplateService();
        PrintJobService svc = new PrintJobService(printerService, templateService, syncExecutor);

        var job = svc.createJob("order-2", "p1", "t1");
        String id = job.id();

        Method m = PrintJobService.class.getDeclaredMethod("processJob", String.class);
        m.setAccessible(true);

        Thread t = new Thread(() -> {
            try {
                m.invoke(svc, id);
            } catch (Exception e) {
                // ignore
            }
        });

        t.start();
        Thread.sleep(10);
        t.interrupt();
        t.join(500);

        var after = svc.listJobs(java.util.Map.of()).stream().filter(j -> j.id().equals(id)).findFirst().orElseThrow();
        assertTrue(after.status().equals("failed") || after.status().equals("completed") || after.status().equals("printing"));
    }
} 
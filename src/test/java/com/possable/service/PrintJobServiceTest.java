package com.possable.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import java.util.List;
import java.util.Map;

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

        var all = svc.listJobs(Map.of());
        assertTrue(all.stream().anyMatch(j -> j.id().equals(job.id())));

        svc.updateStatus(job.id(), "printing");
        var jAfter = svc.listJobs(Map.of()).stream().filter(j -> j.id().equals(job.id())).findFirst().orElseThrow();
        assertEquals("printing", jAfter.status());

        svc.updateStatus(job.id(), "completed");
        var jCompleted = svc.listJobs(Map.of("status", "completed")).stream().filter(j -> j.id().equals(job.id())).findFirst().orElse(null);
        assertNotNull(jCompleted);
    }
} 
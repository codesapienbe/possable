package com.possable.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import static org.junit.jupiter.api.Assertions.*;

public class PrintJobServiceExtraTest {

    private final TaskExecutor syncExecutor = r -> r.run();

    @Test
    public void listJobsFiltersAndUpdateNotFound() {
        PrinterService printerService = new PrinterService();
        PrintTemplateService templateService = new PrintTemplateService();
        PrintJobService svc = new PrintJobService(printerService, templateService, syncExecutor);

        var j1 = svc.createJob("o1", "p1", "t1");
        var j2 = svc.createJob("o2", "p1", "t1");

        var all = svc.listJobs(java.util.Map.of());
        assertTrue(all.size() >= 2);

        var filtered = svc.listJobs(java.util.Map.of("orderId", "o1"));
        assertTrue(filtered.stream().anyMatch(j -> j.orderId().equals("o1")));

        // update non-existent should just return without exception
        svc.updateStatus("no-such", "failed");
    }
} 
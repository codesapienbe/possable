package com.possable.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class PrintJobService {

    private static final Logger log = LoggerFactory.getLogger(PrintJobService.class);

    private final List<PrintJob> jobs = Collections.synchronizedList(new ArrayList<>());
    private final PrinterService printerService;
    private final PrintTemplateService templateService;
    private final TaskExecutor taskExecutor;

    public record PrintJob(String id, String orderId, String printerId, String templateId, String status, Instant createdAt) {}

    public PrintJobService(PrinterService printerService, PrintTemplateService templateService, TaskExecutor taskExecutor) {
        this.printerService = printerService;
        this.templateService = templateService;
        this.taskExecutor = taskExecutor;
    }

    public List<PrintJob> listJobs(Map<String, String> filters) {
        synchronized (jobs) {
            return jobs.stream()
                    .filter(j -> filters == null || filters.isEmpty() || (
                            (filters.get("orderId") == null || filters.get("orderId").equals(j.orderId())) &&
                            (filters.get("status") == null || filters.get("status").equals(j.status()))
                    ))
                    .collect(Collectors.toList());
        }
    }

    public PrintJob createJob(String orderId, String printerId, String templateId) {
        String id = UUID.randomUUID().toString();
        PrintJob job = new PrintJob(id, orderId, printerId, templateId, "pending", Instant.now());
        jobs.add(job);
        log.info("{\"message\":\"print_job_created\", \"print_job_id\":\"{}\", \"component\":\"print-job-service\"}", id);

        // Schedule processing
        CompletableFuture.runAsync(() -> processJob(job.id()), taskExecutor::execute)
                .exceptionally(ex -> {
                    log.error("print job processing failed for {}", id, ex);
                    return null;
                });

        return job;
    }

    public void updateStatus(String jobId, String status) {
        synchronized (jobs) {
            for (int i = 0; i < jobs.size(); i++) {
                var j = jobs.get(i);
                if (j.id().equals(jobId)) {
                    jobs.set(i, new PrintJob(j.id(), j.orderId(), j.printerId(), j.templateId(), status, j.createdAt()));
                    log.info("{\"message\":\"print_job_status_updated\", \"print_job_id\":\"{}\", \"status\":\"{}\", \"component\":\"print-job-service\"}", jobId, status);
                    return;
                }
            }
        }
    }

    private void processJob(String jobId) {
        // Mark printing, sleep, then complete or fail
        try {
            updateStatus(jobId, "printing");
            Thread.sleep(100); // simulate printing I/O
            updateStatus(jobId, "completed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            updateStatus(jobId, "failed");
        }
    }
} 
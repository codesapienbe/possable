package com.possable.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class PrintJobService {

    private static final Logger log = LoggerFactory.getLogger(PrintJobService.class);

    private final List<PrintJob> jobs = Collections.synchronizedList(new ArrayList<>());
    private final PrinterService printerService;
    private final PrintTemplateService templateService;
    private final OrderService orderService;
    private final ItemService itemService;
    private final TaskExecutor taskExecutor;

    public record PrintJob(String id, String orderId, String printerId, String templateId, String status, Instant createdAt) {}

    public PrintJobService(PrinterService printerService, PrintTemplateService templateService, OrderService orderService, ItemService itemService, TaskExecutor taskExecutor) {
        this.printerService = printerService;
        this.templateService = templateService;
        this.orderService = orderService;
        this.itemService = itemService;
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
        // Render template and perform non-blocking "print" (log the output)
        PrintJob job;
        synchronized (jobs) {
            job = jobs.stream().filter(j -> j.id().equals(jobId)).findFirst().orElse(null);
        }
        if (job == null) {
            log.error("{\"message\":\"print_job_not_found\", \"print_job_id\":\"{}\", \"component\":\"print-job-service\"}", jobId);
            return;
        }

        try {
            updateStatus(jobId, "printing");

            var template = templateService.findById(job.templateId());
            var order = orderService.findById(job.orderId());
            var printer = printerService.findById(job.printerId());

            if (template == null) {
                log.warn("{\"message\":\"template_missing\", \"template_id\":\"{}\", \"component\":\"print-job-service\"}", job.templateId());
                updateStatus(jobId, "failed");
                return;
            }
            if (order == null) {
                log.warn("{\"message\":\"order_missing\", \"order_id\":\"{}\", \"component\":\"print-job-service\"}", job.orderId());
                updateStatus(jobId, "failed");
                return;
            }

            // Build items list and total
            StringBuilder itemsBuilder = new StringBuilder();
            double total = 0.0;
            if (order.getItems() != null) {
                for (String itemId : order.getItems()) {
                    var it = itemService.findById(itemId);
                    if (it != null) {
                        itemsBuilder.append(it.name()).append(" x1, ");
                        total += it.price();
                    } else {
                        itemsBuilder.append(itemId).append(" x1, ");
                    }
                }
                if (itemsBuilder.length() > 2) itemsBuilder.setLength(itemsBuilder.length() - 2);
            }

            String rendered = template.content()
                    .replace("{{orderId}}", order.getId())
                    .replace("{{items}}", itemsBuilder.toString())
                    .replace("{{total}}", String.format("$%.2f", total))
                    .replace("{{notes}}", "");

            // Log the rendered content as the print output (structured)
            log.info("{\"message\":\"print_job_executed\", \"print_job_id\":\"{}\", \"printer_id\":\"{}\", \"printer_name\":\"{}\", \"component\":\"print-job-service\", \"output\":\"{}\"}", jobId, printer != null ? printer.id() : job.printerId(), printer != null ? printer.name() : "unknown", rendered.replace("\n", "\\n"));

            updateStatus(jobId, "completed");
        } catch (Exception e) {
            log.error("print job processing error {}", jobId, e);
            updateStatus(jobId, "failed");
        }
    }

} 
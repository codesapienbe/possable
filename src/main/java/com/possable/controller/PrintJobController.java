package com.possable.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.possable.service.PrintJobService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Tag(name = "Print Jobs", description = "Operations to manage print jobs and their SSE stream")
@RestController
@SecurityRequirement(name = "ApiKeyAuth")
@RequestMapping("/print-jobs")
public class PrintJobController {

    private final PrintJobService jobService;

    @Value("${possable.security.api-key:}")
    private String configuredApiKey;

    public PrintJobController(PrintJobService jobService) {
        this.jobService = jobService;
    }

    public static class CreatePrintJobsRequest {
        @NotBlank
        private String orderId;

        @NotNull
        @NotEmpty
        private List<JobItem> jobs;

        public CreatePrintJobsRequest() {}

        public CreatePrintJobsRequest(String orderId, List<JobItem> jobs) {
            this.orderId = orderId;
            this.jobs = jobs;
        }

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }

        public List<JobItem> getJobs() { return jobs; }
        public void setJobs(List<JobItem> jobs) { this.jobs = jobs; }

        public static class JobItem {
            @NotBlank
            private String printerId;

            @NotBlank
            private String templateId;

            public JobItem() {}

            public JobItem(String printerId, String templateId) {
                this.printerId = printerId;
                this.templateId = templateId;
            }

            public String getPrinterId() { return printerId; }
            public void setPrinterId(String printerId) { this.printerId = printerId; }

            public String getTemplateId() { return templateId; }
            public void setTemplateId(String templateId) { this.templateId = templateId; }
        }
    }

    @Operation(summary = "Create print jobs for an order", description = "Create print jobs targeting specific printers with templates for an order.")
    @PostMapping
    public ResponseEntity<List<PrintJobService.PrintJob>> createPrintJobs(@Valid @RequestBody CreatePrintJobsRequest req) {
        var created = req.getJobs().stream()
                .map(j -> jobService.createJob(req.getOrderId(), j.getPrinterId(), j.getTemplateId()))
                .collect(Collectors.toList());
        return ResponseEntity.status(201).body(created);
    }

    @Operation(summary = "List print jobs", description = "List print jobs with optional filters by orderId or status and support pagination via page and limit query params.")
    @GetMapping
    public ResponseEntity<List<PrintJobService.PrintJob>> listJobs(
            @Parameter(description = "Filter jobs by order ID") @RequestParam(required = false) String orderId,
            @Parameter(description = "Filter jobs by print status", schema = @Schema(allowableValues = {"pending", "printing", "completed", "failed"})) @RequestParam(required = false) String status,
            @Parameter(description = "Page index (0-based)") @RequestParam(required = false) Integer page,
            @Parameter(description = "Max number of items per page") @RequestParam(required = false) Integer limit) {
        var filters = new java.util.HashMap<String, String>();
        if (orderId != null && !orderId.isBlank()) filters.put("orderId", orderId);
        if (status != null && !status.isBlank()) filters.put("status", status);
        if (page != null) filters.put("page", Integer.toString(Math.max(0, page)));
        if (limit != null) filters.put("limit", Integer.toString(Math.max(1, limit)));
        return ResponseEntity.ok(jobService.listJobs(filters));
    }

    @Operation(summary = "Subscribe to print job events", description = "Open an SSE stream subscribing to the given topics. Topics that reference specific orders/jobs/printers require an API key.")
    @GetMapping("/stream")
    public ResponseEntity<SseEmitter> stream(@RequestParam(required = false) String topics, @RequestHeader(value = "X-API-KEY", required = false) String apiKey) {
        String topicCsv = topics == null || topics.isBlank() ? "all" : topics;
        // Require API key for sensitive topic subscriptions (order:, job:, printer:)
        boolean requiresApiKey = java.util.Arrays.stream(topicCsv.split(",")).map(String::trim).anyMatch(t -> t.startsWith("order:") || t.startsWith("job:") || t.startsWith("printer:"));
        if (requiresApiKey) {
            if (apiKey == null || apiKey.isBlank() || configuredApiKey == null || configuredApiKey.isBlank() || !configuredApiKey.equals(apiKey)) {
                return ResponseEntity.status(403).build();
            }
        }
        SseEmitter emitter = jobService.createEmitterForTopics(topicCsv);
        return ResponseEntity.ok(emitter);
    }

    @Operation(summary = "SSE metrics for print jobs", description = "Get metrics about SSE collapsed/dropped/total events")
    @GetMapping("/metrics/sse")
    public ResponseEntity<?> sseMetrics() {
        return ResponseEntity.ok(Map.of(
                "collapsedEvents", jobService.getCollapsedEvents(),
                "droppedEmitters", jobService.getDroppedEmitters(),
                "totalSent", jobService.getTotalSent()
        ));
    }

    public static class UpdateStatusRequest {
        @NotBlank
        private String status;

        public UpdateStatusRequest() {}
        public UpdateStatusRequest(String status) { this.status = status; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    @Operation(summary = "Update print job status", description = "Update the status of a print job (e.g. pending, printing, completed, failed)")
    @PutMapping("/{printJobId}")
    public ResponseEntity<Map<String, String>> updateStatus(@PathVariable String printJobId, @Valid @RequestBody UpdateStatusRequest req) {
        jobService.updateStatus(printJobId, req.getStatus());
        return ResponseEntity.ok(Map.of("id", printJobId, "status", req.getStatus()));
    }
} 
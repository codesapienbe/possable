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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@RestController
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

    @PostMapping
    public ResponseEntity<List<PrintJobService.PrintJob>> createPrintJobs(@Valid @RequestBody CreatePrintJobsRequest req) {
        var created = req.getJobs().stream()
                .map(j -> jobService.createJob(req.getOrderId(), j.getPrinterId(), j.getTemplateId()))
                .collect(Collectors.toList());
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping
    public ResponseEntity<java.util.Map<String,Object>> listJobs(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit) {
        var filters = new java.util.HashMap<String, String>();
        if (orderId != null && !orderId.isBlank()) filters.put("orderId", orderId);
        if (status != null && !status.isBlank()) filters.put("status", status);
        if (page != null) filters.put("page", Integer.toString(Math.max(0, page)));
        if (limit != null) filters.put("limit", Integer.toString(Math.max(1, limit)));
        return ResponseEntity.ok(jobService.listJobsPaged(filters));
    }

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

    @PutMapping("/{printJobId}")
    public ResponseEntity<Map<String, String>> updateStatus(@PathVariable String printJobId, @Valid @RequestBody UpdateStatusRequest req) {
        jobService.updateStatus(printJobId, req.getStatus());
        return ResponseEntity.ok(Map.of("id", printJobId, "status", req.getStatus()));
    }
} 
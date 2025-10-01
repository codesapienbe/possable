package com.possable.print.controller;

import java.time.Instant;
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

import com.possable.print.PrintFacade;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/print-jobs")
public class PrintJobController {

    private final PrintFacade printFacade;

    @Value("${possable.security.api-key:}")
    private String configuredApiKey;

    public PrintJobController(PrintFacade printFacade) {
        this.printFacade = printFacade;
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

    public static class PrintJobDto {
        private String id;
        private String orderId;
        private String printerId;
        private String templateId;
        private String status;
        private Instant createdAt;
        
        public PrintJobDto() {}
        public PrintJobDto(String id, String orderId, String printerId, String templateId, String status, Instant createdAt) {
            this.id = id;
            this.orderId = orderId;
            this.printerId = printerId;
            this.templateId = templateId;
            this.status = status;
            this.createdAt = createdAt;
        }
        
        public String getId() { return id; }
        public String getOrderId() { return orderId; }
        public String getPrinterId() { return printerId; }
        public String getTemplateId() { return templateId; }
        public String getStatus() { return status; }
        public Instant getCreatedAt() { return createdAt; }
        
        // Record-style accessors for backwards compatibility
        public String id() { return id; }
        public String orderId() { return orderId; }
        public String printerId() { return printerId; }
        public String templateId() { return templateId; }
        public String status() { return status; }
        public Instant createdAt() { return createdAt; }
    }

    @PostMapping
    public ResponseEntity<List<PrintJobDto>> createPrintJobs(@Valid @RequestBody CreatePrintJobsRequest req) {
        var created = req.getJobs().stream()
                .map(j -> {
                    var job = printFacade.createJob(req.getOrderId(), j.getPrinterId(), j.getTemplateId());
                    return new PrintJobDto(job.id(), job.orderId(), job.printerId(), job.templateId(), job.status(), job.createdAt());
                })
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
        
        // Convert facade response to controller DTOs
        var jobs = printFacade.listJobs(filters).stream()
            .map(j -> new PrintJobDto(j.id(), j.orderId(), j.printerId(), j.templateId(), j.status(), j.createdAt()))
            .toList();
        
        return ResponseEntity.ok(Map.of("items", jobs));
    }

    @GetMapping("/stream")
    public ResponseEntity<SseEmitter> stream(@RequestParam(required = false) String topics, @RequestHeader(value = "X-API-KEY", required = false) String apiKey) {
        String topicCsv = topics == null || topics.isBlank() ? "all" : topics;
        boolean requiresApiKey = java.util.Arrays.stream(topicCsv.split(",")).map(String::trim).anyMatch(t -> t.startsWith("order:") || t.startsWith("job:") || t.startsWith("printer:"));
        if (requiresApiKey) {
            if (apiKey == null || apiKey.isBlank() || configuredApiKey == null || configuredApiKey.isBlank() || !configuredApiKey.equals(apiKey)) {
                return ResponseEntity.status(403).build();
            }
        }
        SseEmitter emitter = printFacade.createEmitterForTopics(topicCsv);
        return ResponseEntity.ok(emitter);
    }

    @GetMapping("/metrics/sse")
    public ResponseEntity<?> sseMetrics() {
        return ResponseEntity.ok(Map.of(
                "collapsedEvents", printFacade.getCollapsedEvents(),
                "droppedEmitters", printFacade.getDroppedEmitters(),
                "totalSent", printFacade.getTotalSent()
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
        printFacade.updateStatus(printJobId, req.getStatus());
        return ResponseEntity.ok(Map.of("id", printJobId, "status", req.getStatus()));
    }
} 

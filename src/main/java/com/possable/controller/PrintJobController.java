package com.possable.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.possable.service.PrintJobService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@RestController
@SecurityRequirement(name = "ApiKeyAuth")
@RequestMapping("/print-jobs")
public class PrintJobController {

    private final PrintJobService jobService;

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
    public ResponseEntity<List<PrintJobService.PrintJob>> listJobs(@RequestParam(required = false) String orderId, @RequestParam(required = false) String status) {
        var filters = Map.ofEntries(
                orderId == null ? Map.entry("", "") : Map.entry("orderId", orderId),
                status == null ? Map.entry("", "") : Map.entry("status", status)
        ).entrySet().stream().filter(e -> e.getKey() != null && !e.getKey().isEmpty()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return ResponseEntity.ok(jobService.listJobs(filters));
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
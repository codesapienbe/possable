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

    public record CreatePrintJobsRequest(@NotBlank String orderId, @NotNull @NotEmpty List<JobItem> jobs) {
        public static record JobItem(@NotBlank String printerId, @NotBlank String templateId) {}
    }

    @PostMapping
    public ResponseEntity<List<PrintJobService.PrintJob>> createPrintJobs(@Valid @RequestBody CreatePrintJobsRequest req) {
        var created = req.jobs().stream()
                .map(j -> jobService.createJob(req.orderId(), j.printerId(), j.templateId()))
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

    public record UpdateStatusRequest(@NotBlank String status) {}

    @PutMapping("/{printJobId}")
    public ResponseEntity<Map<String, String>> updateStatus(@PathVariable String printJobId, @Valid @RequestBody UpdateStatusRequest req) {
        jobService.updateStatus(printJobId, req.status());
        return ResponseEntity.ok(Map.of("id", printJobId, "status", req.status()));
    }
} 
package com.possable.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.possable.service.UsageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Usage", description = "API usage and limits")
@RestController
@SecurityRequirement(name = "ApiKeyAuth")
@RequestMapping("/usage")
public class UsageController {

    private final UsageService usageService;

    public UsageController(UsageService usageService) {
        this.usageService = usageService;
    }

    @Operation(summary = "Get current API usage and limits")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUsage() {
        return ResponseEntity.ok(Map.of(
                "monthlyLimit", usageService.getMonthlyLimit(),
                "requestsMade", usageService.getRequestsMade(),
                "resetDate", usageService.getResetAt().toString()
        ));
    }
} 
package com.possable.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.possable.service.UsageService;

@RestController
@RequestMapping("/usage")
public class UsageController {

    private final UsageService usageService;

    public UsageController(UsageService usageService) {
        this.usageService = usageService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getUsage() {
        return ResponseEntity.ok(Map.of(
                "monthlyLimit", usageService.getMonthlyLimit(),
                "requestsMade", usageService.getRequestsMade(),
                "resetDate", usageService.getResetAt().toString()
        ));
    }
} 
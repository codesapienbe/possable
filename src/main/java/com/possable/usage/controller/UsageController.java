package com.possable.usage.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.possable.usage.UsageFacade;

@RestController
@RequestMapping("/usage")
public class UsageController {

    private final UsageFacade usageFacade;

    public UsageController(UsageFacade usageFacade) {
        this.usageFacade = usageFacade;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getUsage() {
        var m = usageFacade.getUsage();
        return ResponseEntity.ok(Map.of(
                "monthlyLimit", m.get("monthlyLimit"),
                "requestsMade", m.get("requestsMade"),
                "resetDate", m.get("resetAt") == null ? null : m.get("resetAt").toString()
        ));
    }
} 
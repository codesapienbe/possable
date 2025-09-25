package com.possable.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.possable.service.PrinterService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@SecurityRequirement(name = "ApiKeyAuth")
@RequestMapping("/printers")
public class PrinterController {

    private final PrinterService printerService;

    public PrinterController(PrinterService printerService) {
        this.printerService = printerService;
    }

    @GetMapping
    public ResponseEntity<List<PrinterService.Printer>> listPrinters() {
        return ResponseEntity.ok(printerService.listPrinters());
    }

    public static class RegisterPrinterRequest {
        @NotBlank
        private String name;
        @NotBlank
        private String category;
        private String description;
        public RegisterPrinterRequest() {}
        public RegisterPrinterRequest(String name, String category, String description) { this.name = name; this.category = category; this.description = description; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> registerPrinter(@Valid @RequestBody RegisterPrinterRequest req) {
        var p = printerService.registerPrinter(req.getName(), req.getCategory(), req.getDescription());
        return ResponseEntity.status(201).body(Map.of("id", p.id(), "name", p.name(), "category", p.category()));
    }
} 
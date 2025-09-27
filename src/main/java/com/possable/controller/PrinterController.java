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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@Tag(name = "Printers", description = "Manage printers")
@RestController
@SecurityRequirement(name = "ApiKeyAuth")
@RequestMapping("/printers")
public class PrinterController {

    private final PrinterService printerService;

    public PrinterController(PrinterService printerService) {
        this.printerService = printerService;
    }

    @Operation(summary = "List all printers", responses = {@ApiResponse(responseCode = "200", description = "List of printers", content = @Content(schema = @Schema(implementation = PrinterService.Printer.class)))})
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

    @Operation(summary = "Register a new printer")
    @PostMapping
    public ResponseEntity<Map<String, Object>> registerPrinter(@Valid @RequestBody RegisterPrinterRequest req) {
        var p = printerService.registerPrinter(req.getName(), req.getCategory(), req.getDescription());
        return ResponseEntity.status(201).body(Map.of("id", p.id(), "name", p.name(), "category", p.category()));
    }
} 
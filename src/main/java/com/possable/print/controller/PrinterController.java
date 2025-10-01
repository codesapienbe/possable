package com.possable.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.possable.print.PrintFacade;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/printers")
public class PrinterController {

    private final PrintFacade printFacade;

    public PrinterController(PrintFacade printFacade) {
        this.printFacade = printFacade;
    }

    public static class RegisterPrinterRequest {
        @NotBlank
        private String name;
        @NotBlank
        private String category;
        private String description;
        
        public RegisterPrinterRequest() {}
        public RegisterPrinterRequest(String name, String category, String description) { 
            this.name = name; 
            this.category = category; 
            this.description = description; 
        }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class PrinterDto {
        private String id;
        private String name;
        private String category;
        private String description;
        private Instant createdAt;
        
        public PrinterDto() {}
        public PrinterDto(String id, String name, String category, String description, Instant createdAt) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.description = description;
            this.createdAt = createdAt;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public String getDescription() { return description; }
        public Instant getCreatedAt() { return createdAt; }
        
        // Record-style accessors for backwards compatibility
        public String id() { return id; }
        public String name() { return name; }
        public String category() { return category; }
        public String description() { return description; }
        public Instant createdAt() { return createdAt; }
    }

    @GetMapping
    public ResponseEntity<List<PrinterDto>> listPrinters(@RequestParam(required = false) String category) {
        var filters = new java.util.HashMap<String, String>();
        if (category != null && !category.isBlank()) {
            filters.put("category", category);
        }
        
        var printers = printFacade.listPrinters(filters).stream()
            .map(p -> new PrinterDto(p.id(), p.name(), p.category(), p.description(), p.createdAt()))
            .toList();
        
        return ResponseEntity.ok(printers);
    }

    @GetMapping("/{printerId}")
    public ResponseEntity<PrinterDto> getPrinter(@PathVariable String printerId) {
        var printer = printFacade.findPrinterById(printerId);
        if (printer == null) return ResponseEntity.notFound().build();
        
        var dto = new PrinterDto(printer.id(), printer.name(), printer.category(), printer.description(), printer.createdAt());
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> registerPrinter(@Valid @RequestBody RegisterPrinterRequest req) {
        var printer = printFacade.registerPrinter(req.getName(), req.getCategory(), req.getDescription());
        return ResponseEntity.status(201).body(Map.of(
            "id", printer.id(), 
            "name", printer.name(), 
            "category", printer.category()
        ));
    }
} 
package com.possable.controller;

import java.time.Instant;
import java.util.List;

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
@RequestMapping("/print-templates")
public class PrintTemplateController {

    private final PrintFacade printFacade;

    public PrintTemplateController(PrintFacade printFacade) {
        this.printFacade = printFacade;
    }

    public static class CreateTemplateRequest {
        @NotBlank
        private String printerCategory;
        @NotBlank
        private String templateName;
        @NotBlank
        private String content;
        
        public CreateTemplateRequest() {}
        public CreateTemplateRequest(String printerCategory, String templateName, String content) { 
            this.printerCategory = printerCategory; 
            this.templateName = templateName; 
            this.content = content; 
        }
        
        public String getPrinterCategory() { return printerCategory; }
        public void setPrinterCategory(String printerCategory) { this.printerCategory = printerCategory; }
        public String getTemplateName() { return templateName; }
        public void setTemplateName(String templateName) { this.templateName = templateName; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public static record TemplateDto(String id, String printerCategory, String templateName, String content, Instant createdAt) {
        // Backwards-compatible bean-style getters
        public String getId() { return id(); }
        public String getPrinterCategory() { return printerCategory(); }
        public String getTemplateName() { return templateName(); }
        public String getContent() { return content(); }
        public Instant getCreatedAt() { return createdAt(); }
    }

    @GetMapping
    public ResponseEntity<List<TemplateDto>> listTemplates(@RequestParam(required = false) String category) {
        var filters = new java.util.HashMap<String, String>();
        if (category != null && !category.isBlank()) {
            filters.put("category", category);
        }
        
        var templates = printFacade.listTemplates(filters).stream()
            .map(t -> new TemplateDto(t.id(), t.printerCategory(), t.templateName(), t.content(), t.createdAt()))
            .toList();
        
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<TemplateDto> getTemplate(@PathVariable String templateId) {
        var template = printFacade.findTemplateById(templateId);
        if (template == null) return ResponseEntity.notFound().build();
        
        var dto = new TemplateDto(template.id(), template.printerCategory(), template.templateName(), 
            template.content(), template.createdAt());
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<TemplateDto> createTemplate(@Valid @RequestBody CreateTemplateRequest req) {
        var template = printFacade.createTemplate(req.getPrinterCategory(), req.getTemplateName(), req.getContent());
        var dto = new TemplateDto(template.id(), template.printerCategory(), template.templateName(), 
            template.content(), template.createdAt());
        return ResponseEntity.status(201).body(dto);
    }
} 
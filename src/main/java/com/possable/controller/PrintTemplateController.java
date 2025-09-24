package com.possable.controller;

import com.possable.service.PrintTemplateService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/print-templates")
public class PrintTemplateController {

    private final PrintTemplateService templateService;

    public PrintTemplateController(PrintTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public ResponseEntity<List<PrintTemplateService.Template>> listTemplates() {
        return ResponseEntity.ok(templateService.listTemplates());
    }

    public record CreateTemplateRequest(@NotBlank String printerCategory, @NotBlank String templateName, @NotBlank String content) {}

    @PostMapping
    public ResponseEntity<PrintTemplateService.Template> createTemplate(@Valid @RequestBody CreateTemplateRequest req) {
        var t = templateService.createTemplate(req.printerCategory(), req.templateName(), req.content());
        return ResponseEntity.status(201).body(t);
    }
} 
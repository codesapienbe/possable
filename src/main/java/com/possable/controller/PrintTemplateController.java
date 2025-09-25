package com.possable.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.possable.service.PrintTemplateService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@SecurityRequirement(name = "ApiKeyAuth")
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
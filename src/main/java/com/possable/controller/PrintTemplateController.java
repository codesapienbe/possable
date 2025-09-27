package com.possable.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.possable.service.PrintTemplateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@Tag(name = "Print Templates", description = "Manage print templates")
@RestController
@SecurityRequirement(name = "ApiKeyAuth")
@RequestMapping("/print-templates")
public class PrintTemplateController {

    private final PrintTemplateService templateService;

    public PrintTemplateController(PrintTemplateService templateService) {
        this.templateService = templateService;
    }

    @Operation(summary = "List print templates", responses = {@ApiResponse(responseCode = "200", description = "List of templates", content = @Content(schema = @Schema(implementation = PrintTemplateService.Template.class)))})
    @GetMapping
    public ResponseEntity<List<PrintTemplateService.Template>> listTemplates() {
        return ResponseEntity.ok(templateService.listTemplates());
    }

    public static class CreateTemplateRequest {
        @NotBlank
        private String printerCategory;
        @NotBlank
        private String templateName;
        @NotBlank
        private String content;
        public CreateTemplateRequest() {}
        public CreateTemplateRequest(String printerCategory, String templateName, String content) { this.printerCategory = printerCategory; this.templateName = templateName; this.content = content; }
        public String getPrinterCategory() { return printerCategory; }
        public void setPrinterCategory(String printerCategory) { this.printerCategory = printerCategory; }
        public String getTemplateName() { return templateName; }
        public void setTemplateName(String templateName) { this.templateName = templateName; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    @Operation(summary = "Create a new print template")
    @PostMapping
    public ResponseEntity<PrintTemplateService.Template> createTemplate(@Valid @RequestBody CreateTemplateRequest req) {
        var t = templateService.createTemplate(req.getPrinterCategory(), req.getTemplateName(), req.getContent());
        return ResponseEntity.status(201).body(t);
    }
} 
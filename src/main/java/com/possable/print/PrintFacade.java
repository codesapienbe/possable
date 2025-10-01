package com.possable.print;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.possable.print.internal.PrintModuleService;

/**
 * Public API facade for the Print module.
 * Manages print jobs, printers, and templates.
 */
@Service
public class PrintFacade {

    private final PrintModuleService printModuleService;

    public record PrintJobInfo(String id, String orderId, String printerId, String templateId, String status, Instant createdAt) {}
    public record PrinterInfo(String id, String name, String category, String description, Instant createdAt) {}
    public record TemplateInfo(String id, String printerCategory, String templateName, String content, Instant createdAt) {}

    public PrintFacade(PrintModuleService printModuleService) {
        this.printModuleService = printModuleService;
    }

    // Print Job Operations
    public PrintJobInfo createJob(String orderId, String printerId, String templateId) {
        var job = printModuleService.createJob(orderId, printerId, templateId);
        return new PrintJobInfo(job.id(), job.orderId(), job.printerId(), job.templateId(), job.status(), job.createdAt());
    }

    public void updateStatus(String jobId, String status) {
        printModuleService.updateStatus(jobId, status);
    }

    public List<PrintJobInfo> listJobs(Map<String, String> filters) {
        return printModuleService.listJobs(filters).stream()
            .map(j -> new PrintJobInfo(j.id(), j.orderId(), j.printerId(), j.templateId(), j.status(), j.createdAt()))
            .toList();
    }

    // Printer Operations
    public PrinterInfo registerPrinter(String name, String category, String description) {
        var printer = printModuleService.registerPrinter(name, category, description);
        return new PrinterInfo(printer.id(), printer.name(), printer.category(), printer.description(), printer.createdAt());
    }

    public List<PrinterInfo> listPrinters(Map<String, String> filters) {
        return printModuleService.listPrinters(filters).stream()
            .map(p -> new PrinterInfo(p.id(), p.name(), p.category(), p.description(), p.createdAt()))
            .toList();
    }

    public PrinterInfo findPrinterById(String id) {
        var printer = printModuleService.findPrinterById(id);
        return printer != null 
            ? new PrinterInfo(printer.id(), printer.name(), printer.category(), printer.description(), printer.createdAt())
            : null;
    }

    // Template Operations
    public TemplateInfo createTemplate(String printerCategory, String templateName, String content) {
        var template = printModuleService.createTemplate(printerCategory, templateName, content);
        return new TemplateInfo(template.id(), template.printerCategory(), template.templateName(), template.content(), template.createdAt());
    }

    public List<TemplateInfo> listTemplates(Map<String, String> filters) {
        return printModuleService.listTemplates(filters).stream()
            .map(t -> new TemplateInfo(t.id(), t.printerCategory(), t.templateName(), t.content(), t.createdAt()))
            .toList();
    }

    public TemplateInfo findTemplateById(String id) {
        var template = printModuleService.findTemplateById(id);
        return template != null 
            ? new TemplateInfo(template.id(), template.printerCategory(), template.templateName(), template.content(), template.createdAt())
            : null;
    }

    // SSE and metrics passthrough
    public SseEmitter createEmitterForTopics(String topicCsv) {
        return printModuleService.createEmitterForTopics(topicCsv);
    }

    public double getCollapsedEvents() { return printModuleService.getCollapsedEvents(); }
    public double getDroppedEmitters() { return printModuleService.getDroppedEmitters(); }
    public double getTotalSent() { return printModuleService.getTotalSent(); }
} 
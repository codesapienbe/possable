package com.possable.print.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.possable.order.OrderCreatedEvent;
import com.possable.print.PrintJobRequestedEvent;
import com.possable.service.Broadcaster;

/**
 * Internal service for print module.
 * Manages print jobs, printers, and templates - WRITES to print module tables.
 */
@Service
public class PrintModuleService {

    private static final Logger log = LoggerFactory.getLogger(PrintModuleService.class);

    private final List<PrintJob> inMemoryJobs = Collections.synchronizedList(new ArrayList<>());
    private final List<Printer> inMemoryPrinters = Collections.synchronizedList(new ArrayList<>());
    private final List<Template> inMemoryTemplates = Collections.synchronizedList(new ArrayList<>());
    private final TaskExecutor taskExecutor;
    private final PrintJobRepository printJobRepository;
    private final PrinterRepository printerRepository;
    private final PrintTemplateRepository templateRepository;

    public record PrintJob(String id, String orderId, String printerId, String templateId, String status, Instant createdAt) {}
    public record Printer(String id, String name, String category, String description, Instant createdAt) {}
    public record Template(String id, String printerCategory, String templateName, String content, Instant createdAt) {}

    public PrintModuleService(
            TaskExecutor taskExecutor,
            PrintJobRepository printJobRepository,
            PrinterRepository printerRepository,
            PrintTemplateRepository templateRepository) {
        this.taskExecutor = taskExecutor;
        this.printJobRepository = printJobRepository;
        this.printerRepository = printerRepository;
        this.templateRepository = templateRepository;
    }

    /**
     * Listen to order created events and automatically create print jobs
     */
    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("{\"message\":\"order_created_event_received\", \"order_id\":\"{}\", \"component\":\"print-module\", \"timestamp\":\"{}\"}", 
            event.id(), Instant.now());
        
        // Auto-create print jobs for common printer categories
        var printers = listPrinters(Map.of());
        if (!printers.isEmpty()) {
            var kitchenPrinters = printers.stream()
                .filter(p -> "kitchen".equalsIgnoreCase(p.category()))
                .toList();
            
            if (!kitchenPrinters.isEmpty()) {
                var printer = kitchenPrinters.get(0);
                var templates = listTemplates(Map.of("category", "kitchen"));
                if (!templates.isEmpty()) {
                    createJob(event.id(), printer.id(), templates.get(0).id());
                }
            }
        }
    }

    /**
     * Listen to print job requested events
     */
    @EventListener
    public void onPrintJobRequested(PrintJobRequestedEvent event) {
        log.info("{\"message\":\"print_job_requested_event_received\", \"order_id\":\"{}\", \"categories\":\"{}\", \"component\":\"print-module\", \"timestamp\":\"{}\"}", 
            event.orderId(), event.printerCategories(), Instant.now());
        
        for (String category : event.printerCategories()) {
            var printers = listPrinters(Map.of("category", category));
            var templates = listTemplates(Map.of("category", category));
            
            if (!printers.isEmpty() && !templates.isEmpty()) {
                createJob(event.orderId(), printers.get(0).id(), templates.get(0).id());
            }
        }
    }

    @Transactional
    public PrintJob createJob(String orderId, String printerId, String templateId) {
        if (printJobRepository != null) {
            PrintJobEntity e = new PrintJobEntity();
            e.setOrderId(orderId);
            e.setPrinterId(printerId);
            e.setTemplateId(templateId);
            e.setStatus("pending");
            e.setCreatedAt(Instant.now());
            
            PrintJobEntity saved = printJobRepository.save(e);
            PrintJob job = new PrintJob(saved.getId(), saved.getOrderId(), saved.getPrinterId(), 
                saved.getTemplateId(), saved.getStatus(), saved.getCreatedAt());
            
            log.info("{\"message\":\"print_job_created\", \"print_job_id\":\"{}\", \"order_id\":\"{}\", \"printer_id\":\"{}\", \"component\":\"print-module\", \"timestamp\":\"{}\"}", 
                job.id(), orderId, printerId, Instant.now());

            String createdEvent = "{\"id\":\"" + job.id() + "\", \"status\":\"pending\"}";
            Broadcaster.broadcast(createdEvent);

            CompletableFuture.runAsync(() -> processJob(job.id()), taskExecutor::execute)
                .exceptionally(ex -> {
                    log.error("{\"message\":\"print_job_processing_failed\", \"print_job_id\":\"{}\", \"component\":\"print-module\", \"error\":\"{}\"}", 
                        job.id(), sanitize(ex.getMessage()), ex);
                    return null;
                });
            
            return job;
        }

        String id = UUID.randomUUID().toString();
        PrintJob job = new PrintJob(id, orderId, printerId, templateId, "pending", Instant.now());
        inMemoryJobs.add(job);
        
        log.info("{\"message\":\"print_job_created\", \"print_job_id\":\"{}\", \"order_id\":\"{}\", \"component\":\"print-module\", \"timestamp\":\"{}\"}", 
            id, orderId, Instant.now());

        String createdEvent = "{\"id\":\"" + id + "\", \"status\":\"pending\"}";
        Broadcaster.broadcast(createdEvent);

        CompletableFuture.runAsync(() -> processJob(id), taskExecutor::execute)
            .exceptionally(ex -> {
                log.error("{\"message\":\"print_job_processing_failed\", \"print_job_id\":\"{}\", \"component\":\"print-module\", \"error\":\"{}\"}", 
                    id, sanitize(ex.getMessage()), ex);
                return null;
            });

        return job;
    }

    @Transactional
    public void updateStatus(String jobId, String status) {
        if (printJobRepository != null) {
            var opt = printJobRepository.findById(jobId);
            if (opt.isPresent()) {
                PrintJobEntity e = opt.get();
                e.setStatus(status);
                PrintJobEntity saved = printJobRepository.save(e);
                
                log.info("{\"message\":\"print_job_status_updated\", \"print_job_id\":\"{}\", \"status\":\"{}\", \"component\":\"print-module\", \"timestamp\":\"{}\"}", 
                    jobId, status, Instant.now());
                
                String ev = "{\"id\":\"" + jobId + "\", \"status\":\"" + status + "\"}";
                Broadcaster.broadcast(ev);
                return;
            }
            return;
        }
        
        synchronized (inMemoryJobs) {
            for (int i = 0; i < inMemoryJobs.size(); i++) {
                var j = inMemoryJobs.get(i);
                if (j.id().equals(jobId)) {
                    inMemoryJobs.set(i, new PrintJob(j.id(), j.orderId(), j.printerId(), j.templateId(), status, j.createdAt()));
                    
                    log.info("{\"message\":\"print_job_status_updated\", \"print_job_id\":\"{}\", \"status\":\"{}\", \"component\":\"print-module\", \"timestamp\":\"{}\"}", 
                        jobId, status, Instant.now());
                    
                    String ev = "{\"id\":\"" + jobId + "\", \"status\":\"" + status + "\"}";
                    Broadcaster.broadcast(ev);
                    return;
                }
            }
        }
    }

    private void processJob(String jobId) {
        PrintJob job = null;
        if (printJobRepository != null) {
            var opt = printJobRepository.findById(jobId);
            if (opt.isPresent()) {
                var e = opt.get();
                job = new PrintJob(e.getId(), e.getOrderId(), e.getPrinterId(), e.getTemplateId(), e.getStatus(), e.getCreatedAt());
            }
        } else {
            synchronized (inMemoryJobs) {
                job = inMemoryJobs.stream()
                    .filter(j -> j.id().equals(jobId))
                    .findFirst()
                    .orElse(null);
            }
        }

        if (job == null) {
            log.error("{\"message\":\"print_job_not_found\", \"print_job_id\":\"{}\", \"component\":\"print-module\"}", jobId);
            return;
        }

        try {
            updateStatus(jobId, "printing");
            
            var template = findTemplateById(job.templateId());
            var printer = findPrinterById(job.printerId());

            if (template == null) {
                log.warn("{\"message\":\"template_missing\", \"template_id\":\"{}\", \"component\":\"print-module\"}", job.templateId());
                updateStatus(jobId, "failed");
                return;
            }

            String rendered = template.content()
                .replace("{{orderId}}", job.orderId())
                .replace("{{jobId}}", job.id());

            log.info("{\"message\":\"print_job_executed\", \"print_job_id\":\"{}\", \"printer_id\":\"{}\", \"printer_name\":\"{}\", \"component\":\"print-module\", \"output\":\"{}\"}", 
                jobId, 
                printer != null ? printer.id() : job.printerId(),
                printer != null ? sanitize(printer.name()) : "unknown",
                sanitize(rendered.replace("\n", "\\n")));

            updateStatus(jobId, "completed");
        } catch (Exception e) {
            log.error("{\"message\":\"print_job_processing_error\", \"print_job_id\":\"{}\", \"component\":\"print-module\", \"error\":\"{}\"}", 
                jobId, sanitize(e.getMessage()), e);
            updateStatus(jobId, "failed");
        }
    }

    @Transactional(readOnly = true)
    public List<PrintJob> listJobs(Map<String, String> filters) {
        if (printJobRepository != null) {
            int page = 0;
            int size = 100;
            if (filters != null) {
                try { 
                    if (filters.containsKey("page")) {
                        page = Math.max(0, Integer.parseInt(filters.get("page"))); 
                    }
                } catch (Exception ignored) {}
                try { 
                    if (filters.containsKey("limit")) {
                        size = Math.max(1, Math.min(1000, Integer.parseInt(filters.get("limit")))); 
                    }
                } catch (Exception ignored) {}
            }
            
            String orderId = filters != null ? filters.get("orderId") : null;
            String status = filters != null ? filters.get("status") : null;
            var pageable = org.springframework.data.domain.PageRequest.of(page, size);
            
            org.springframework.data.domain.Page<PrintJobEntity> pageRes;
            if (orderId != null && status != null) {
                pageRes = printJobRepository.findByOrderIdAndStatus(orderId, status, pageable);
            } else if (orderId != null) {
                pageRes = printJobRepository.findByOrderId(orderId, pageable);
            } else if (status != null) {
                pageRes = printJobRepository.findByStatus(status, pageable);
            } else {
                pageRes = printJobRepository.findAll(pageable);
            }
            
            return pageRes.stream()
                .map(e -> new PrintJob(e.getId(), e.getOrderId(), e.getPrinterId(), e.getTemplateId(), e.getStatus(), e.getCreatedAt()))
                .collect(Collectors.toList());
        }
        
        synchronized (inMemoryJobs) {
            return inMemoryJobs.stream()
                .filter(j -> filters == null || filters.isEmpty() || 
                    ((filters.get("orderId") == null || filters.get("orderId").equals(j.orderId())) && 
                     (filters.get("status") == null || filters.get("status").equals(j.status()))))
                .collect(Collectors.toList());
        }
    }

    // ============= PRINTER MANAGEMENT =============

    @Transactional
    public Printer registerPrinter(String name, String category, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Printer name cannot be null or empty");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Printer category cannot be null or empty");
        }

        if (printerRepository != null) {
            PrinterEntity e = new PrinterEntity();
            e.setName(name);
            e.setCategory(category);
            e.setDescription(description);
            e.setCreatedAt(Instant.now());
            
            PrinterEntity saved = printerRepository.save(e);
            log.info("{\"message\":\"printer_registered\", \"printer_id\":\"{}\", \"name\":\"{}\", \"category\":\"{}\", \"component\":\"print-module\", \"timestamp\":\"{}\"}", 
                saved.getId(), sanitize(name), sanitize(category), Instant.now());
            
            return new Printer(saved.getId(), saved.getName(), saved.getCategory(), saved.getDescription(), saved.getCreatedAt());
        }
        
        String id = UUID.randomUUID().toString();
        Printer p = new Printer(id, name, category, description, Instant.now());
        inMemoryPrinters.add(p);
        
        log.info("{\"message\":\"printer_registered\", \"printer_id\":\"{}\", \"name\":\"{}\", \"category\":\"{}\", \"component\":\"print-module\", \"timestamp\":\"{}\"}", 
            id, sanitize(name), sanitize(category), Instant.now());
        
        return p;
    }

    @Transactional(readOnly = true)
    public List<Printer> listPrinters(Map<String, String> filters) {
        if (printerRepository != null) {
            String category = filters != null ? filters.get("category") : null;
            if (category != null && !category.isBlank()) {
                return printerRepository.findByCategory(category).stream()
                    .map(e -> new Printer(e.getId(), e.getName(), e.getCategory(), e.getDescription(), e.getCreatedAt()))
                    .collect(Collectors.toList());
            }
            return printerRepository.findAll().stream()
                .map(e -> new Printer(e.getId(), e.getName(), e.getCategory(), e.getDescription(), e.getCreatedAt()))
                .collect(Collectors.toList());
        }
        
        synchronized (inMemoryPrinters) {
            if (filters != null && filters.containsKey("category")) {
                String category = filters.get("category");
                return inMemoryPrinters.stream()
                    .filter(p -> category.equalsIgnoreCase(p.category()))
                    .toList();
            }
            return List.copyOf(inMemoryPrinters);
        }
    }

    @Transactional(readOnly = true)
    public Printer findPrinterById(String id) {
        if (printerRepository != null) {
            return printerRepository.findById(id)
                .map(e -> new Printer(e.getId(), e.getName(), e.getCategory(), e.getDescription(), e.getCreatedAt()))
                .orElse(null);
        }
        
        synchronized (inMemoryPrinters) {
            return inMemoryPrinters.stream()
                .filter(p -> p.id().equals(id))
                .findFirst()
                .orElse(null);
        }
    }

    // ============= TEMPLATE MANAGEMENT =============

    @Transactional
    public Template createTemplate(String printerCategory, String templateName, String content) {
        if (printerCategory == null || printerCategory.isBlank()) {
            throw new IllegalArgumentException("Printer category cannot be null or empty");
        }
        if (templateName == null || templateName.isBlank()) {
            throw new IllegalArgumentException("Template name cannot be null or empty");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Template content cannot be null or empty");
        }

        if (templateRepository != null) {
            PrintTemplateEntity e = new PrintTemplateEntity();
            e.setPrinterCategory(printerCategory);
            e.setTemplateName(templateName);
            e.setContent(content);
            e.setCreatedAt(Instant.now());
            
            PrintTemplateEntity saved = templateRepository.save(e);
            log.info("{\"message\":\"print_template_created\", \"template_id\":\"{}\", \"name\":\"{}\", \"category\":\"{}\", \"component\":\"print-module\", \"timestamp\":\"{}\"}", 
                saved.getId(), sanitize(templateName), sanitize(printerCategory), Instant.now());
            
            return new Template(saved.getId(), saved.getPrinterCategory(), saved.getTemplateName(), saved.getContent(), saved.getCreatedAt());
        }
        
        String id = UUID.randomUUID().toString();
        Template t = new Template(id, printerCategory, templateName, content, Instant.now());
        inMemoryTemplates.add(t);
        
        log.info("{\"message\":\"print_template_created\", \"template_id\":\"{}\", \"name\":\"{}\", \"category\":\"{}\", \"component\":\"print-module\", \"timestamp\":\"{}\"}", 
            id, sanitize(templateName), sanitize(printerCategory), Instant.now());
        
        return t;
    }

    @Transactional(readOnly = true)
    public List<Template> listTemplates(Map<String, String> filters) {
        if (templateRepository != null) {
            String category = filters != null ? filters.get("category") : null;
            if (category != null && !category.isBlank()) {
                return templateRepository.findByPrinterCategory(category).stream()
                    .map(e -> new Template(e.getId(), e.getPrinterCategory(), e.getTemplateName(), e.getContent(), e.getCreatedAt()))
                    .collect(Collectors.toList());
            }
            return templateRepository.findAll().stream()
                .map(e -> new Template(e.getId(), e.getPrinterCategory(), e.getTemplateName(), e.getContent(), e.getCreatedAt()))
                .collect(Collectors.toList());
        }
        
        synchronized (inMemoryTemplates) {
            if (filters != null && filters.containsKey("category")) {
                String category = filters.get("category");
                return inMemoryTemplates.stream()
                    .filter(t -> category.equalsIgnoreCase(t.printerCategory()))
                    .toList();
            }
            return List.copyOf(inMemoryTemplates);
        }
    }

    @Transactional(readOnly = true)
    public Template findTemplateById(String id) {
        if (templateRepository != null) {
            return templateRepository.findById(id)
                .map(e -> new Template(e.getId(), e.getPrinterCategory(), e.getTemplateName(), e.getContent(), e.getCreatedAt()))
                .orElse(null);
        }
        
        synchronized (inMemoryTemplates) {
            return inMemoryTemplates.stream()
                .filter(t -> t.id().equals(id))
                .findFirst()
                .orElse(null);
        }
    }

    private String sanitize(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
} 



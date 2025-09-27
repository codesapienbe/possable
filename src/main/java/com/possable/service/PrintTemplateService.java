package com.possable.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.possable.model.PrintTemplateEntity;
import com.possable.repository.PrintTemplateRepository;

@Service
public class PrintTemplateService {

    private static final Logger log = LoggerFactory.getLogger(PrintTemplateService.class);

    private final PrintTemplateRepository templateRepository;
    private final List<Template> inMemoryTemplates = Collections.synchronizedList(new ArrayList<>());

    public record Template(String id, String printerCategory, String templateName, String content, Instant createdAt) {}

    @Autowired
    public PrintTemplateService(PrintTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    // no-arg constructor used by tests to run in in-memory mode
    public PrintTemplateService() {
        this.templateRepository = null;
    }

    private Template toRecord(PrintTemplateEntity e) {
        if (e == null) return null;
        return new Template(e.getId(), e.getPrinterCategory(), e.getTemplateName(), e.getContent(), e.getCreatedAt());
    }

    private PrintTemplateEntity toEntity(String printerCategory, String templateName, String content) {
        PrintTemplateEntity e = new PrintTemplateEntity();
        e.setPrinterCategory(printerCategory);
        e.setTemplateName(templateName);
        e.setContent(content);
        e.setCreatedAt(Instant.now());
        return e;
    }

    @Transactional
    public Template createTemplate(String printerCategory, String templateName, String content) {
        if (templateRepository != null) {
            PrintTemplateEntity e = toEntity(printerCategory, templateName, content);
            PrintTemplateEntity saved = templateRepository.save(e);
            log.info("{\"message\":\"print_template_created\", \"template_id\":\"{}\", \"component\":\"print-template-service\"}", saved.getId());
            return toRecord(saved);
        }
        String id = UUID.randomUUID().toString();
        Template t = new Template(id, printerCategory, templateName, content, Instant.now());
        inMemoryTemplates.add(t);
        log.info("{\"message\":\"print_template_created\", \"template_id\":\"{}\", \"component\":\"print-template-service\"}", id);
        return t;
    }

    @Transactional(readOnly = true)
    public List<Template> listTemplates() {
        if (templateRepository != null) {
            return templateRepository.findAll().stream().map(this::toRecord).collect(Collectors.toList());
        }
        synchronized (inMemoryTemplates) {
            return List.copyOf(inMemoryTemplates);
        }
    }

    @Transactional(readOnly = true)
    public Template findById(String id) {
        if (templateRepository != null) {
            return templateRepository.findById(id).map(this::toRecord).orElse(null);
        }
        synchronized (inMemoryTemplates) {
            return inMemoryTemplates.stream().filter(t -> t.id().equals(id)).findFirst().orElse(null);
        }
    }
} 
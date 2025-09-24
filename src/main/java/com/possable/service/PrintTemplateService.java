package com.possable.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class PrintTemplateService {

    private static final Logger log = LoggerFactory.getLogger(PrintTemplateService.class);

    private final List<Template> templates = Collections.synchronizedList(new ArrayList<>());

    public record Template(String id, String printerCategory, String templateName, String content, Instant createdAt) {}

    public Template createTemplate(String printerCategory, String templateName, String content) {
        String id = UUID.randomUUID().toString();
        Template t = new Template(id, printerCategory, templateName, content, Instant.now());
        templates.add(t);
        log.info("{\"message\":\"print_template_created\", \"template_id\":\"{}\", \"component\":\"print-template-service\"}", id);
        return t;
    }

    public List<Template> listTemplates() {
        synchronized (templates) {
            return List.copyOf(templates);
        }
    }

    public Template findById(String id) {
        synchronized (templates) {
            return templates.stream().filter(t -> t.id().equals(id)).findFirst().orElse(null);
        }
    }
} 
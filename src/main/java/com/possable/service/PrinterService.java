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
public class PrinterService {

    private static final Logger log = LoggerFactory.getLogger(PrinterService.class);

    private final List<Printer> printers = Collections.synchronizedList(new ArrayList<>());

    public record Printer(String id, String name, String category, String description, Instant createdAt) {}

    public Printer registerPrinter(String name, String category, String description) {
        String id = UUID.randomUUID().toString();
        Printer p = new Printer(id, name, category, description, Instant.now());
        printers.add(p);
        log.info("{\"message\":\"printer_registered\", \"printer_id\":\"{}\", \"component\":\"printer-service\"}", id);
        return p;
    }

    public List<Printer> listPrinters() {
        synchronized (printers) {
            return List.copyOf(printers);
        }
    }

    public Printer findById(String id) {
        synchronized (printers) {
            return printers.stream().filter(p -> p.id().equals(id)).findFirst().orElse(null);
        }
    }
} 
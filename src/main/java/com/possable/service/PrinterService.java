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

import com.possable.model.PrinterEntity;
import com.possable.repository.PrinterRepository;

@Service
public class PrinterService {

    private static final Logger log = LoggerFactory.getLogger(PrinterService.class);

    private final PrinterRepository printerRepository;
    private final List<Printer> inMemoryPrinters = Collections.synchronizedList(new ArrayList<>());

    public record Printer(String id, String name, String category, String description, Instant createdAt) {}

    @Autowired
    public PrinterService(PrinterRepository printerRepository) {
        this.printerRepository = printerRepository;
    }

    // no-arg constructor used by tests to run in in-memory mode
    public PrinterService() {
        this.printerRepository = null;
    }

    private Printer toRecord(PrinterEntity e) {
        if (e == null) return null;
        return new Printer(e.getId(), e.getName(), e.getCategory(), e.getDescription(), e.getCreatedAt());
    }

    private PrinterEntity toEntity(String id, String name, String category, String description) {
        PrinterEntity e = new PrinterEntity();
        if (id != null) e.setId(id);
        e.setName(name);
        e.setCategory(category);
        e.setDescription(description);
        e.setCreatedAt(Instant.now());
        return e;
    }

    @Transactional
    public Printer registerPrinter(String name, String category, String description) {
        if (printerRepository != null) {
            PrinterEntity e = toEntity(null, name, category, description);
            PrinterEntity saved = printerRepository.save(e);
            log.info("{\"message\":\"printer_registered\", \"printer_id\":\"{}\", \"component\":\"printer-service\"}", saved.getId());
            return toRecord(saved);
        }
        String id = UUID.randomUUID().toString();
        Printer p = new Printer(id, name, category, description, Instant.now());
        inMemoryPrinters.add(p);
        log.info("{\"message\":\"printer_registered\", \"printer_id\":\"{}\", \"component\":\"printer-service\"}", id);
        return p;
    }

    @Transactional(readOnly = true)
    public List<Printer> listPrinters() {
        if (printerRepository != null) {
            return printerRepository.findAll().stream().map(this::toRecord).collect(Collectors.toList());
        }
        synchronized (inMemoryPrinters) {
            return List.copyOf(inMemoryPrinters);
        }
    }

    @Transactional(readOnly = true)
    public Printer findById(String id) {
        if (printerRepository != null) {
            return printerRepository.findById(id).map(this::toRecord).orElse(null);
        }
        synchronized (inMemoryPrinters) {
            return inMemoryPrinters.stream().filter(p -> p.id().equals(id)).findFirst().orElse(null);
        }
    }
} 
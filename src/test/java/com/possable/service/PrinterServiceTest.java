package com.possable.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PrinterServiceTest {

    @Test
    public void registerAndListAndFindPrinter() {
        PrinterService svc = new PrinterService();

        var p = svc.registerPrinter("HP", "label", "fast printer");
        assertNotNull(p);
        assertNotNull(p.id());
        assertEquals("HP", p.name());

        var all = svc.listPrinters();
        assertTrue(all.stream().anyMatch(pr -> pr.id().equals(p.id())));

        var found = svc.findById(p.id());
        assertNotNull(found);
        assertEquals(p.id(), found.id());
    }
} 
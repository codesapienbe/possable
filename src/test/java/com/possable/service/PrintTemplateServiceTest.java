package com.possable.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PrintTemplateServiceTest {

    @Test
    public void createListAndFindTemplate() {
        PrintTemplateService svc = new PrintTemplateService();

        var t = svc.createTemplate("label", "default", "content");
        assertNotNull(t);
        assertNotNull(t.id());
        assertEquals("default", t.templateName());

        var all = svc.listTemplates();
        assertTrue(all.stream().anyMatch(tt -> tt.id().equals(t.id())));

        var found = svc.findById(t.id());
        assertNotNull(found);
        assertEquals(t.id(), found.id());
    }
} 
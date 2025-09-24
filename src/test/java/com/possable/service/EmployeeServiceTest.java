package com.possable.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EmployeeServiceTest {

    @Test
    public void addAndListEmployees() {
        EmployeeService svc = new EmployeeService();

        var e1 = svc.addEmployee("Alice", "admin", true);
        assertNotNull(e1);
        assertNotNull(e1.id());
        assertEquals("Alice", e1.name());
        assertTrue(e1.active());

        var e2 = svc.addEmployee("Bob", "user", false);
        assertNotNull(e2);

        List<EmployeeService.Employee> list = svc.listEmployees(10);
        assertEquals(2, list.size());

        List<EmployeeService.Employee> limited = svc.listEmployees(1);
        assertEquals(1, limited.size());
    }
} 
package com.possable.employee.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Internal service for employee module.
 * Handles employee management and authentication.
 */
@Service
public class EmployeeModuleService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeModuleService.class);

    private final List<Employee> employees = Collections.synchronizedList(new ArrayList<>());

    public record Employee(String id, String name, String role, boolean active, Instant createdAt) {}

    public Employee addEmployee(String name, String role, boolean active) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Employee name cannot be null or empty");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Employee role cannot be null or empty");
        }
        
        String id = UUID.randomUUID().toString();
        Employee e = new Employee(id, name, role, active, Instant.now());
        employees.add(e);
        
        log.info("{\"message\":\"employee_added\", \"employee_id\":\"{}\", \"name\":\"{}\", \"role\":\"{}\", \"component\":\"employee-module\", \"timestamp\":\"{}\"}", 
            id, name, role, Instant.now());
        
        return e;
    }

    public List<Employee> listEmployees(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Limit cannot be negative");
        }
        
        synchronized (employees) {
            return List.copyOf(employees).stream()
                .limit(Math.min(limit, employees.size()))
                .toList();
        }
    }

    public Employee findById(String id) {
        synchronized (employees) {
            return employees.stream()
                .filter(e -> e.id().equals(id))
                .findFirst()
                .orElse(null);
        }
    }

    public Employee updateEmployee(String id, String name, String role, boolean active) {
        synchronized (employees) {
            for (int i = 0; i < employees.size(); i++) {
                var e = employees.get(i);
                if (e.id().equals(id)) {
                    Employee updated = new Employee(id, name, role, active, e.createdAt());
                    employees.set(i, updated);
                    
                    log.info("{\"message\":\"employee_updated\", \"employee_id\":\"{}\", \"component\":\"employee-module\", \"timestamp\":\"{}\"}", 
                        id, Instant.now());
                    
                    return updated;
                }
            }
            return null;
        }
    }

    public boolean deleteEmployee(String id) {
        synchronized (employees) {
            boolean removed = employees.removeIf(e -> e.id().equals(id));
            if (removed) {
                log.info("{\"message\":\"employee_deleted\", \"employee_id\":\"{}\", \"component\":\"employee-module\", \"timestamp\":\"{}\"}", 
                    id, Instant.now());
            }
            return removed;
        }
    }
} 

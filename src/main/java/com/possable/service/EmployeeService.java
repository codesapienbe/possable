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
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final List<Employee> employees = Collections.synchronizedList(new ArrayList<>());

    public record Employee(String id, String name, String role, boolean active, Instant createdAt) {}

    public Employee addEmployee(String name, String role, boolean active) {
        String id = UUID.randomUUID().toString();
        Employee e = new Employee(id, name, role, active, Instant.now());
        employees.add(e);
        log.info("{\"message\":\"employee_added\", \"employee_id\":\"{}\", \"component\":\"employee-service\"}", id);
        return e;
    }

    public List<Employee> listEmployees(int limit) {
        synchronized (employees) {
            return List.copyOf(employees).stream().limit(Math.max(0, Math.min(limit, employees.size()))).toList();
        }
    }
} 
package com.possable.employee;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.possable.employee.internal.EmployeeModuleService;

/**
 * Public API facade for the Employee module.
 * This is the only class that other modules should depend on.
 */
@Service
public class EmployeeFacade {

    private final EmployeeModuleService employeeModuleService;

    public record EmployeeInfo(String id, String name, String role, boolean active, Instant createdAt) {}

    public EmployeeFacade(EmployeeModuleService employeeModuleService) {
        this.employeeModuleService = employeeModuleService;
    }

    public EmployeeInfo addEmployee(String name, String role, boolean active) {
        var employee = employeeModuleService.addEmployee(name, role, active);
        return new EmployeeInfo(employee.id(), employee.name(), employee.role(), employee.active(), employee.createdAt());
    }

    public List<EmployeeInfo> listEmployees(int limit) {
        return employeeModuleService.listEmployees(limit).stream()
            .map(e -> new EmployeeInfo(e.id(), e.name(), e.role(), e.active(), e.createdAt()))
            .toList();
    }

    public EmployeeInfo findById(String id) {
        var employee = employeeModuleService.findById(id);
        return employee != null 
            ? new EmployeeInfo(employee.id(), employee.name(), employee.role(), employee.active(), employee.createdAt())
            : null;
    }

    public EmployeeInfo updateEmployee(String id, String name, String role, boolean active) {
        var employee = employeeModuleService.updateEmployee(id, name, role, active);
        return employee != null 
            ? new EmployeeInfo(employee.id(), employee.name(), employee.role(), employee.active(), employee.createdAt())
            : null;
    }

    public boolean deleteEmployee(String id) {
        return employeeModuleService.deleteEmployee(id);
    }
} 
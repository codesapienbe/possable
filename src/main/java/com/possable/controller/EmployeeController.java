package com.possable.controller;

import com.possable.service.EmployeeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    public record CreateEmployeeRequest(@NotBlank String name, String role, Boolean active) {}

    @GetMapping
    public ResponseEntity<List<EmployeeService.Employee>> listEmployees(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(employeeService.listEmployees(limit));
    }

    @PostMapping
    public ResponseEntity<EmployeeService.Employee> addEmployee(@Valid @RequestBody CreateEmployeeRequest req) {
        boolean active = req.active() == null ? true : req.active();
        var e = employeeService.addEmployee(req.name(), req.role(), active);
        return ResponseEntity.created(URI.create("/employees/" + e.id())).body(e);
    }
} 
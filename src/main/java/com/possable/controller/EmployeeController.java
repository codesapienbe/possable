package com.possable.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.possable.service.EmployeeService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@SecurityRequirement(name = "ApiKeyAuth")
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
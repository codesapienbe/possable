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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@Tag(name = "Employees", description = "Operations for employee management")
@RestController
@SecurityRequirement(name = "ApiKeyAuth")
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    public static class CreateEmployeeRequest {
        @NotBlank
        private String name;
        private String role;
        private Boolean active;

        public CreateEmployeeRequest() {}
        public CreateEmployeeRequest(String name, String role, Boolean active) { this.name = name; this.role = role; this.active = active; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }

    @Operation(summary = "List employees", responses = {@ApiResponse(responseCode = "200", description = "List of employees", content = @Content(schema = @Schema(implementation = EmployeeService.Employee.class)))})
    @GetMapping
    public ResponseEntity<List<EmployeeService.Employee>> listEmployees(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(employeeService.listEmployees(limit));
    }

    @Operation(summary = "Add a new employee")
    @PostMapping
    public ResponseEntity<EmployeeService.Employee> addEmployee(@Valid @RequestBody CreateEmployeeRequest req) {
        boolean active = req.getActive() == null ? true : req.getActive();
        var e = employeeService.addEmployee(req.getName(), req.getRole(), active);
        return ResponseEntity.created(URI.create("/employees/" + e.id())).body(e);
    }
} 
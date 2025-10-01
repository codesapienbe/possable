package com.possable.controller;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.possable.employee.EmployeeFacade;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeFacade employeeFacade;

    public EmployeeController(EmployeeFacade employeeFacade) {
        this.employeeFacade = employeeFacade;
    }

    public static class CreateEmployeeRequest {
        @NotBlank
        private String name;
        private String role;
        private Boolean active;

        public CreateEmployeeRequest() {}
        public CreateEmployeeRequest(String name, String role, Boolean active) { 
            this.name = name; 
            this.role = role; 
            this.active = active; 
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }

    public static class EmployeeDto {
        private String id;
        private String name;
        private String role;
        private boolean active;
        private Instant createdAt;
        
        public EmployeeDto() {}
        public EmployeeDto(String id, String name, String role, boolean active, Instant createdAt) {
            this.id = id;
            this.name = name;
            this.role = role;
            this.active = active;
            this.createdAt = createdAt;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getRole() { return role; }
        public boolean isActive() { return active; }
        public Instant getCreatedAt() { return createdAt; }
        
        // Record-style accessors for backwards compatibility
        public String id() { return id; }
        public String name() { return name; }
        public String role() { return role; }
        public boolean active() { return active; }
        public Instant createdAt() { return createdAt; }
    }

    @GetMapping
    public ResponseEntity<List<EmployeeDto>> listEmployees(@RequestParam(defaultValue = "20") int limit) {
        var employees = employeeFacade.listEmployees(limit).stream()
            .map(e -> new EmployeeDto(e.id(), e.name(), e.role(), e.active(), e.createdAt()))
            .toList();
        return ResponseEntity.ok(employees);
    }

    @PostMapping
    public ResponseEntity<EmployeeDto> addEmployee(@Valid @RequestBody CreateEmployeeRequest req) {
        boolean active = req.getActive() == null ? true : req.getActive();
        var employee = employeeFacade.addEmployee(req.getName(), req.getRole(), active);
        var dto = new EmployeeDto(employee.id(), employee.name(), employee.role(), employee.active(), employee.createdAt());
        return ResponseEntity.created(URI.create("/employees/" + dto.getId())).body(dto);
    }
} 
package com.possable.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.possable.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class EmployeeControllerTest {

    private MockMvc mockMvc;
    private EmployeeService employeeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        employeeService = new EmployeeService();
        var controller = new EmployeeController(employeeService);
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    public void postAndGetEmployees() throws Exception {
        String json = objectMapper.writeValueAsString(new Object() {
            public final String name = "Charlie";
        });

        mockMvc.perform(post("/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/employees/")))
                .andExpect(jsonPath("$.name").value("Charlie"));

        mockMvc.perform(get("/employees?limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
} 
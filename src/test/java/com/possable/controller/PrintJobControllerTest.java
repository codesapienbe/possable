package com.possable.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.possable.service.PrintJobService;
import com.possable.service.PrinterService;
import com.possable.service.PrintTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.core.task.TaskExecutor;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class PrintJobControllerTest {

    private MockMvc mockMvc;
    private PrintJobService jobService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        TaskExecutor syncExecutor = r -> r.run();
        PrinterService printerService = new PrinterService();
        PrintTemplateService templateService = new PrintTemplateService();
        jobService = new PrintJobService(printerService, templateService, syncExecutor);
        var controller = new PrintJobController(jobService);
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    public void createAndListPrintJobs() throws Exception {
        String json = objectMapper.writeValueAsString(new Object() {
            public final String orderId = "order-x";
            public final List<Object> jobs = List.of(new Object() { public final String printerId = "p1"; public final String templateId = "t1"; });
        });

        mockMvc.perform(post("/print-jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].orderId").value("order-x"));

        mockMvc.perform(get("/print-jobs?orderId=order-x"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value("order-x"));
    }
} 
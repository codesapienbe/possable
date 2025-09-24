package com.possable.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.possable.service.PrinterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class PrinterControllerTest {

    private MockMvc mockMvc;
    private PrinterService printerService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        printerService = new PrinterService();
        var controller = new PrinterController(printerService);
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    public void registerAndListPrinters() throws Exception {
        String json = objectMapper.writeValueAsString(new Object() {
            public final String name = "XPrinter";
            public final String category = "label";
            public final String description = "desc";
        });

        mockMvc.perform(post("/printers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("XPrinter"))
                .andExpect(jsonPath("$.category").value("label"));

        mockMvc.perform(get("/printers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("XPrinter"));
    }
} 
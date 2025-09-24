package com.possable.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.possable.service.PrintTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class PrintTemplateControllerTest {

    private MockMvc mockMvc;
    private PrintTemplateService templateService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        templateService = new PrintTemplateService();
        var controller = new PrintTemplateController(templateService);
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    public void createAndListTemplates() throws Exception {
        String json = objectMapper.writeValueAsString(new Object() {
            public final String printerCategory = "label";
            public final String templateName = "t1";
            public final String content = "c";
        });

        mockMvc.perform(post("/print-templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.templateName").value("t1"));

        mockMvc.perform(get("/print-templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].templateName").value("t1"));
    }
} 
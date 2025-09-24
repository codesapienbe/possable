package com.possable.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.possable.service.UsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class UsageControllerTest {

    private MockMvc mockMvc;
    private UsageService usageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        usageService = new UsageService();
        var controller = new UsageController(usageService);
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    public void getUsageShowsCounters() throws Exception {
        usageService.incrementRequests(5);
        mockMvc.perform(get("/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyLimit").isNumber())
                .andExpect(jsonPath("$.requestsMade").value(5))
                .andExpect(jsonPath("$.resetDate").isString());
    }
} 
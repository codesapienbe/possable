package com.possable.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.possable.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.core.task.TaskExecutor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class PaymentControllerTest {

    private MockMvc mockMvc;
    private PaymentService paymentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        TaskExecutor syncExecutor = r -> r.run();
        paymentService = new PaymentService(syncExecutor);
        var controller = new PaymentController(paymentService);
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    public void createAndGetPayment() throws Exception {
        String json = objectMapper.writeValueAsString(new Object() { public final String orderId = "o1"; public final Double amount = 5.0; public final String method = "card"; });

        var res = mockMvc.perform(post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        var body = objectMapper.readTree(res.getResponse().getContentAsString());
        String id = body.get("id").asText();

        mockMvc.perform(get("/payments/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("o1"));
    }

    @Test
    public void getNotFound() throws Exception {
        mockMvc.perform(get("/payments/nope"))
                .andExpect(status().isNotFound());
    }
} 
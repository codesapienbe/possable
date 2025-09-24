package com.possable.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.possable.service.OrderService;
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

public class OrderControllerTest {

    private MockMvc mockMvc;
    private OrderService orderService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        TaskExecutor syncExecutor = r -> r.run();
        orderService = new OrderService(syncExecutor);
        var controller = new OrderController(orderService);
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    public void createGetAndUpdateOrder() throws Exception {
        String createJson = objectMapper.writeValueAsString(new Object() {
            public final List<String> items = List.of("i1", "i2");
            public final String notes = "note";
        });

        var create = mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/orders/")))
                .andReturn();

        String resp = create.getResponse().getContentAsString();
        var node = objectMapper.readTree(resp);
        String id = node.get("id").asText();

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(get("/orders/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        String updateJson = objectMapper.writeValueAsString(new Object() { public final String status = "COMPLETED"; });
        mockMvc.perform(put("/orders/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
} 
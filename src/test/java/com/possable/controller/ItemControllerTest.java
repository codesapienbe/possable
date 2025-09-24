package com.possable.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.possable.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ItemControllerTest {

    private MockMvc mockMvc;
    private ItemService itemService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        itemService = new ItemService();
        var controller = new ItemController(itemService);
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    public void createGetUpdateDeleteItem() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of("name", "i1", "description", "d", "price", 1.23));

        var res = mockMvc.perform(post("/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        var body = objectMapper.readTree(res.getResponse().getContentAsString());
        String id = body.get("id").asText();

        mockMvc.perform(get("/items/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("i1"));

        String updateJson = objectMapper.writeValueAsString(Map.of("name", "i1b", "description", "d2", "price", 2.34));
        mockMvc.perform(put("/items/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("i1b"));

        mockMvc.perform(delete("/items/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/items/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    public void notFoundFlows() throws Exception {
        mockMvc.perform(get("/items/nope"))
                .andExpect(status().isNotFound());

        String updateJson = objectMapper.writeValueAsString(Map.of("name", "n", "description", "d", "price", 1.0));
        mockMvc.perform(put("/items/nope")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/items/nope"))
                .andExpect(status().isNotFound());
    }
} 
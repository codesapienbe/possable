package com.possable.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.possable.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ItemListControllerTest {

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
    public void listItemsDefaultAndLimit() throws Exception {
        // create three items
        itemService.createItem("a","d",1.0,true);
        itemService.createItem("b","d",2.0,true);
        itemService.createItem("c","d",3.0,true);

        mockMvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(get("/items?limit=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
} 
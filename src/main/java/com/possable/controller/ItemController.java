package com.possable.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.possable.service.ItemService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@RestController
@SecurityRequirement(name = "ApiKeyAuth")
@RequestMapping("/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    public static class CreateItemRequest {
        @NotBlank
        private String name;
        private String description;
        @NotNull
        private Double price;
        private Boolean available;

        public CreateItemRequest() {}
        public CreateItemRequest(String name, String description, Double price, Boolean available) { this.name = name; this.description = description; this.price = price; this.available = available; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public Boolean getAvailable() { return available; }
        public void setAvailable(Boolean available) { this.available = available; }
    }

    @GetMapping
    public ResponseEntity<List<ItemService.Item>> listItems(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(itemService.listItems(limit));
    }

    @PostMapping
    public ResponseEntity<ItemService.Item> createItem(@Valid @RequestBody CreateItemRequest req) {
        boolean available = req.getAvailable() == null ? true : req.getAvailable();
        var it = itemService.createItem(req.getName(), req.getDescription(), req.getPrice(), available);
        return ResponseEntity.created(URI.create("/items/" + it.id())).body(it);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<ItemService.Item> getItem(@PathVariable String itemId) {
        var it = itemService.findById(itemId);
        if (it == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(it);
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<ItemService.Item> updateItem(@PathVariable String itemId, @Valid @RequestBody CreateItemRequest req) {
        boolean available = req.getAvailable() == null ? true : req.getAvailable();
        var updated = itemService.updateItem(itemId, req.getName(), req.getDescription(), req.getPrice(), available);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable String itemId) {
        boolean removed = itemService.deleteItem(itemId);
        if (!removed) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }
} 
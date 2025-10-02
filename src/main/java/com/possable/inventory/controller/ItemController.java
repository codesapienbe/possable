package com.possable.inventory.controller;

import java.net.URI;
import java.time.Instant;

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

import com.possable.inventory.InventoryFacade;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/items")
public class ItemController {

    private final InventoryFacade inventoryFacade;

    public ItemController(InventoryFacade inventoryFacade) {
        this.inventoryFacade = inventoryFacade;
    }

    public static class CreateItemRequest {
        @NotBlank
        private String name;
        private String description;
        @NotNull
        private Double price;
        private Boolean available;
        private String category;
        private String tagsCsv;

        public CreateItemRequest() {}
        public CreateItemRequest(String name, String description, Double price, Boolean available) { 
            this.name = name; 
            this.description = description; 
            this.price = price; 
            this.available = available; 
        }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public Boolean getAvailable() { return available; }
        public void setAvailable(Boolean available) { this.available = available; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getTagsCsv() { return tagsCsv; }
        public void setTagsCsv(String tagsCsv) { this.tagsCsv = tagsCsv; }
    }

    public static record ItemDto(String id, String name, String description, double price, boolean available, Instant createdAt) {
        // Backwards-compatible bean-style getters
        public String getId() { return id(); }
        public String getName() { return name(); }
        public String getDescription() { return description(); }
        public double getPrice() { return price(); }
        public boolean isAvailable() { return available(); }
        public Instant getCreatedAt() { return createdAt(); }
    }

    @GetMapping
    public ResponseEntity<java.util.Map<String,Object>> listItems(
            @RequestParam(required = false) Integer page,
            @RequestParam(defaultValue = "20") int limit) {
        var filters = new java.util.HashMap<String, String>();
        if (page != null) filters.put("page", Integer.toString(Math.max(0, page)));
        if (limit > 0) filters.put("limit", Integer.toString(Math.max(1, Math.min(100, limit))));
        return ResponseEntity.ok(inventoryFacade.listItemsPaged(filters));
    }

    @PostMapping
    public ResponseEntity<ItemDto> createItem(@Valid @RequestBody CreateItemRequest req) {
        boolean available = req.getAvailable() == null ? true : req.getAvailable();
        var item = inventoryFacade.createItem(req.getName(), req.getDescription(), req.getPrice(), available);
        // set optional metadata via updateMetadata if provided
        try {
            if (req.getCategory() != null || req.getTagsCsv() != null) {
                inventoryFacade.updateMetadata(item.id(), req.getCategory(), req.getTagsCsv());
            }
        } catch (Exception ignored) {}
        var dto = new ItemDto(item.id(), item.name(), item.description(), item.price(), item.available(), item.createdAt());
        return ResponseEntity.created(URI.create("/items/" + dto.id())).body(dto);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<ItemDto> getItem(@PathVariable String itemId) {
        var item = inventoryFacade.findById(itemId);
        if (item == null) return ResponseEntity.notFound().build();
        var dto = new ItemDto(item.id(), item.name(), item.description(), item.price(), item.available(), item.createdAt());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<ItemDto> updateItem(@PathVariable String itemId, @Valid @RequestBody CreateItemRequest req) {
        boolean available = req.getAvailable() == null ? true : req.getAvailable();
        var item = inventoryFacade.updateItem(itemId, req.getName(), req.getDescription(), req.getPrice(), available);
        if (item == null) return ResponseEntity.notFound().build();
        try {
            if (req.getCategory() != null || req.getTagsCsv() != null) {
                inventoryFacade.updateMetadata(itemId, req.getCategory(), req.getTagsCsv());
            }
        } catch (Exception ignored) {}
        var dto = new ItemDto(item.id(), item.name(), item.description(), item.price(), item.available(), item.createdAt());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable String itemId) {
        boolean removed = inventoryFacade.deleteItem(itemId);
        if (!removed) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }
} 
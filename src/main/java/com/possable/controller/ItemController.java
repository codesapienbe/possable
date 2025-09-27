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

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Tag(name = "Items", description = "Operations for menu items and catalog")
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

    @Operation(summary = "List menu items", description = "Get a paged list of menu items.", responses = {
        @ApiResponse(responseCode = "200", description = "Paged list of items", content = @Content(schema = @Schema(ref = "#/components/schemas/PagedItem")))
    })
    @GetMapping
    public ResponseEntity<java.util.Map<String,Object>> listItems(
            @Parameter(description = "Page index (0-based)") @RequestParam(required = false) Integer page,
            @Parameter(description = "Max number of items per page") @RequestParam(defaultValue = "20") int limit) {
        var filters = new java.util.HashMap<String, String>();
        if (page != null) filters.put("page", Integer.toString(Math.max(0, page)));
        if (limit > 0) filters.put("limit", Integer.toString(Math.max(1, Math.min(100, limit))));
        return ResponseEntity.ok(itemService.listItemsPaged(filters));
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
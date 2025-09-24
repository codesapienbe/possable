package com.possable.controller;

import com.possable.service.ItemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    public record CreateItemRequest(@NotBlank String name, String description, @NotNull Double price, Boolean available) {}

    @GetMapping
    public ResponseEntity<List<ItemService.Item>> listItems(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(itemService.listItems(limit));
    }

    @PostMapping
    public ResponseEntity<ItemService.Item> createItem(@Valid @RequestBody CreateItemRequest req) {
        boolean available = req.available() == null ? true : req.available();
        var it = itemService.createItem(req.name(), req.description(), req.price(), available);
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
        boolean available = req.available() == null ? true : req.available();
        var updated = itemService.updateItem(itemId, req.name(), req.description(), req.price(), available);
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
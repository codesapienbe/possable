package com.possable.inventory;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.possable.inventory.internal.InventoryModuleService;

/**
 * Public API facade for the Inventory module.
 * Manages menu items and pricing.
 */
@Service
public class InventoryFacade {

    private final InventoryModuleService inventoryModuleService;

    public record ItemInfo(String id, String name, String description, double price, boolean available, Instant createdAt, String category, String tagsCsv) {}

    public InventoryFacade(InventoryModuleService inventoryModuleService) {
        this.inventoryModuleService = inventoryModuleService;
    }

    public ItemInfo createItem(String name, String description, double price, boolean available) {
        var item = inventoryModuleService.createItem(name, description, price, available);
        return new ItemInfo(item.id(), item.name(), item.description(), item.price(), item.available(), item.createdAt(), item.category(), item.tagsCsv());
    }

    public List<ItemInfo> listItems(int limit) {
        return inventoryModuleService.listItems(limit).stream()
            .map(i -> new ItemInfo(i.id(), i.name(), i.description(), i.price(), i.available(), i.createdAt(), i.category(), i.tagsCsv()))
            .toList();
    }

    public ItemInfo findById(String id) {
        var item = inventoryModuleService.findById(id);
        return item != null 
            ? new ItemInfo(item.id(), item.name(), item.description(), item.price(), item.available(), item.createdAt(), item.category(), item.tagsCsv())
            : null;
    }

    public ItemInfo updateItem(String id, String name, String description, double price, boolean available) {
        var item = inventoryModuleService.updateItem(id, name, description, price, available);
        return item != null 
            ? new ItemInfo(item.id(), item.name(), item.description(), item.price(), item.available(), item.createdAt(), item.category(), item.tagsCsv())
            : null;
    }

    public boolean deleteItem(String id) {
        return inventoryModuleService.deleteItem(id);
    }

    public Map<String, Object> listItemsPaged(Map<String, String> filters) {
        return inventoryModuleService.listItemsPaged(filters);
    }

    public void updateMetadata(String id, String category, String tagsCsv) {
        inventoryModuleService.updateMetadata(id, category, tagsCsv);
    }
} 
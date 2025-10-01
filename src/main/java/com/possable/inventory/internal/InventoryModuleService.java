package com.possable.inventory.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Internal service for inventory module.
 * Handles item/menu management - WRITES to items table.
 */
@Service
public class InventoryModuleService {

    private static final Logger log = LoggerFactory.getLogger(InventoryModuleService.class);

    private final ItemRepository itemRepository;
    private final List<Item> inMemoryItems = Collections.synchronizedList(new ArrayList<>());

    public record Item(String id, String name, String description, double price, boolean available, Instant createdAt) {}

    @Autowired
    public InventoryModuleService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public InventoryModuleService() {
        this.itemRepository = null;
    }

    @Transactional
    public Item createItem(String name, String description, double price, boolean available) {
        if (itemRepository != null) {
            ItemEntity e = new ItemEntity();
            e.setName(name);
            e.setDescription(description);
            e.setPrice(BigDecimal.valueOf(price));
            e.setAvailable(available);
            e.setCreatedAt(Instant.now());
            ItemEntity saved = itemRepository.save(e);
            
            log.info("{\"message\":\"item_created\", \"item_id\":\"{}\", \"name\":\"{}\", \"price\":{}, \"component\":\"inventory-module\", \"timestamp\":\"{}\"}", 
                saved.getId(), sanitize(name), price, Instant.now());
            
            return new Item(saved.getId(), saved.getName(), saved.getDescription(), 
                saved.getPrice().doubleValue(), saved.isAvailable(), saved.getCreatedAt());
        }

        String id = UUID.randomUUID().toString();
        Item it = new Item(id, name, description, price, available, Instant.now());
        inMemoryItems.add(it);
        
        log.info("{\"message\":\"item_created\", \"item_id\":\"{}\", \"name\":\"{}\", \"component\":\"inventory-module\", \"timestamp\":\"{}\"}", 
            id, sanitize(name), Instant.now());
        
        return it;
    }

    @Transactional(readOnly = true)
    public List<Item> listItems(int limit) {
        if (itemRepository != null) {
            List<Item> out = new ArrayList<>();
            List<ItemEntity> ents = itemRepository.findAll();
            for (ItemEntity e : ents) {
                out.add(new Item(e.getId(), e.getName(), e.getDescription(), 
                    e.getPrice() == null ? 0.0 : e.getPrice().doubleValue(), 
                    e.isAvailable(), e.getCreatedAt()));
            }
            return out.size() <= limit ? out : out.subList(0, Math.max(0, Math.min(limit, out.size())));
        }
        
        synchronized (inMemoryItems) {
            return List.copyOf(inMemoryItems).stream()
                .limit(Math.max(0, Math.min(limit, inMemoryItems.size())))
                .toList();
        }
    }

    @Transactional(readOnly = true)
    public Item findById(String id) {
        if (itemRepository != null) {
            Optional<ItemEntity> opt = itemRepository.findById(id);
            if (opt.isPresent()) {
                ItemEntity e = opt.get();
                return new Item(e.getId(), e.getName(), e.getDescription(), 
                    e.getPrice() == null ? 0.0 : e.getPrice().doubleValue(), 
                    e.isAvailable(), e.getCreatedAt());
            }
            return null;
        }
        
        synchronized (inMemoryItems) {
            return inMemoryItems.stream()
                .filter(i -> i.id().equals(id))
                .findFirst()
                .orElse(null);
        }
    }

    @Transactional
    public Item updateItem(String id, String name, String description, double price, boolean available) {
        if (itemRepository != null) {
            Optional<ItemEntity> opt = itemRepository.findById(id);
            if (opt.isPresent()) {
                ItemEntity e = opt.get();
                e.setName(name);
                e.setDescription(description);
                e.setPrice(BigDecimal.valueOf(price));
                e.setAvailable(available);
                ItemEntity saved = itemRepository.save(e);
                
                log.info("{\"message\":\"item_updated\", \"item_id\":\"{}\", \"component\":\"inventory-module\", \"timestamp\":\"{}\"}", 
                    id, Instant.now());
                
                return new Item(saved.getId(), saved.getName(), saved.getDescription(), 
                    saved.getPrice().doubleValue(), saved.isAvailable(), saved.getCreatedAt());
            }
            return null;
        }
        
        synchronized (inMemoryItems) {
            for (int i = 0; i < inMemoryItems.size(); i++) {
                var it = inMemoryItems.get(i);
                if (it.id().equals(id)) {
                    Item updated = new Item(id, name, description, price, available, it.createdAt());
                    inMemoryItems.set(i, updated);
                    
                    log.info("{\"message\":\"item_updated\", \"item_id\":\"{}\", \"component\":\"inventory-module\", \"timestamp\":\"{}\"}", 
                        id, Instant.now());
                    
                    return updated;
                }
            }
            return null;
        }
    }

    @Transactional
    public boolean deleteItem(String id) {
        if (itemRepository != null) {
            if (itemRepository.existsById(id)) {
                itemRepository.deleteById(id);
                log.info("{\"message\":\"item_deleted\", \"item_id\":\"{}\", \"component\":\"inventory-module\", \"timestamp\":\"{}\"}", 
                    id, Instant.now());
                return true;
            }
            return false;
        }
        
        synchronized (inMemoryItems) {
            boolean removed = inMemoryItems.removeIf(i -> i.id().equals(id));
            if (removed) {
                log.info("{\"message\":\"item_deleted\", \"item_id\":\"{}\", \"component\":\"inventory-module\", \"timestamp\":\"{}\"}", 
                    id, Instant.now());
            }
            return removed;
        }
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> listItemsPaged(java.util.Map<String, String> filters) {
        int page = 0;
        int size = 20;
        if (filters != null) {
            try { 
                if (filters.containsKey("page")) {
                    page = Math.max(0, Integer.parseInt(filters.get("page"))); 
                }
            } catch (Exception ignored) {}
            try { 
                if (filters.containsKey("limit")) {
                    size = Math.max(1, Math.min(100, Integer.parseInt(filters.get("limit")))); 
                }
            } catch (Exception ignored) {}
        }
        
        if (itemRepository != null) {
            var pageable = org.springframework.data.domain.PageRequest.of(page, size);
            org.springframework.data.domain.Page<ItemEntity> pageRes = itemRepository.findAll(pageable);
            List<Item> items = pageRes.stream()
                .map(e -> new Item(e.getId(), e.getName(), e.getDescription(), 
                    e.getPrice() == null ? 0.0 : e.getPrice().doubleValue(), 
                    e.isAvailable(), e.getCreatedAt()))
                .collect(Collectors.toList());
                
            java.util.Map<String,Object> out = new java.util.LinkedHashMap<>();
            out.put("items", items);
            out.put("page", pageRes.getNumber());
            out.put("size", pageRes.getSize());
            out.put("totalElements", pageRes.getTotalElements());
            out.put("totalPages", pageRes.getTotalPages());
            return out;
        }
        
        List<Item> all;
        synchronized (inMemoryItems) {
            all = List.copyOf(inMemoryItems);
        }
        long totalElements = all.size();
        int from = Math.min(all.size(), page * size);
        int to = Math.min(all.size(), from + size);
        List<Item> pageItems = all.subList(from, to);
        
        java.util.Map<String,Object> out = new java.util.LinkedHashMap<>();
        out.put("items", pageItems);
        out.put("page", page);
        out.put("size", size);
        out.put("totalElements", totalElements);
        out.put("totalPages", (int) Math.ceil((double) totalElements / size));
        return out;
    }

    private String sanitize(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"").replace("\n", "\\n");
    }
} 

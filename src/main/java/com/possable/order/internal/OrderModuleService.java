package com.possable.order.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.possable.order.OrderCompletedEvent;
import com.possable.order.OrderCreatedEvent;

/**
 * Internal service for order module.
 * Handles order creation and publishes domain events for other modules.
 */
@Service
public class OrderModuleService {

    private static final Logger log = LoggerFactory.getLogger(OrderModuleService.class);

    private final List<OrderDto> inMemoryOrders = Collections.synchronizedList(new ArrayList<>());
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ApplicationEventPublisher eventPublisher;

    public record OrderDto(String id, List<String> items, String status, Instant createdAt) {}

    public OrderModuleService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrderDto createOrder(List<String> items, String notes) {
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        
        if (orderRepository != null) {
            OrderEntity order = new OrderEntity();
            order.setStatus("PENDING");
            order.setNotes(notes);
            order.setCreatedAt(now);
            
            for (String itemId : items) {
                OrderItemEntity oi = new OrderItemEntity();
                oi.setItemId(itemId);
                oi.setQuantity(1);
                order.addItem(oi);
            }
            
            OrderEntity saved = orderRepository.save(order);
            log.info("{\"message\":\"order_created\", \"order_id\":\"{}\", \"items_count\":{}, \"component\":\"order-module\", \"timestamp\":\"{}\"}", 
                saved.id(), items.size(), Instant.now());
            
            OrderDto dto = new OrderDto(
                saved.id(),
                saved.items().stream().map(OrderItemEntity::itemId).collect(Collectors.toList()),
                saved.status(),
                saved.getCreatedAt()
            );
            
            // Publish domain event for other modules
            OrderCreatedEvent event = new OrderCreatedEvent(
                saved.id(),
                items,
                notes,
                saved.status(),
                saved.getCreatedAt()
            );
            eventPublisher.publishEvent(event);
            log.debug("{\"message\":\"order_created_event_published\", \"order_id\":\"{}\", \"component\":\"order-module\"}", saved.id());
            
            return dto;
        }

        OrderDto order = new OrderDto(id, List.copyOf(items), "PENDING", now);
        inMemoryOrders.add(order);
        
        log.info("{\"message\":\"order_created\", \"order_id\":\"{}\", \"items_count\":{}, \"component\":\"order-module\", \"timestamp\":\"{}\"}", 
            id, items.size(), Instant.now());
        
        OrderCreatedEvent event = new OrderCreatedEvent(id, items, notes, "PENDING", now);
        eventPublisher.publishEvent(event);
        
        return order;
    }

    @Transactional(readOnly = true)
    public List<OrderDto> listOrders() {
        if (orderRepository != null) {
            List<OrderDto> out = new ArrayList<>();
            for (OrderEntity e : orderRepository.findAll()) {
                List<String> itemIds = e.items() == null ? List.of() 
                    : e.items().stream().map(OrderItemEntity::itemId).collect(Collectors.toList());
                out.add(new OrderDto(e.id(), itemIds, e.status(), e.getCreatedAt()));
            }
            return out;
        }
        synchronized (inMemoryOrders) {
            return List.copyOf(inMemoryOrders);
        }
    }

    @Transactional(readOnly = true)
    public OrderDto findById(String id) {
        if (orderRepository != null) {
            return orderRepository.findById(id)
                .map(e -> new OrderDto(
                    e.id(),
                    e.items() == null ? List.of() 
                        : e.items().stream().map(OrderItemEntity::itemId).collect(Collectors.toList()),
                    e.status(),
                    e.getCreatedAt()
                ))
                .orElse(null);
        }
        synchronized (inMemoryOrders) {
            return inMemoryOrders.stream()
                .filter(o -> o.id().equals(id))
                .findFirst()
                .orElse(null);
        }
    }

    @Transactional
    public OrderDto updateStatus(String id, String status) {
        if (orderRepository != null) {
            var opt = orderRepository.findById(id);
            if (opt.isPresent()) {
                OrderEntity e = opt.get();
                String oldStatus = e.status();
                e.setStatus(status);
                OrderEntity saved = orderRepository.save(e);
                
                log.info("{\"message\":\"order_status_updated\", \"order_id\":\"{}\", \"old_status\":\"{}\", \"new_status\":\"{}\", \"component\":\"order-module\", \"timestamp\":\"{}\"}", 
                    id, oldStatus, status, Instant.now());
                
                // Publish completed event if status changed to completed
                if ("COMPLETED".equalsIgnoreCase(status) && !"COMPLETED".equalsIgnoreCase(oldStatus)) {
                    OrderCompletedEvent event = new OrderCompletedEvent(id, status, Instant.now());
                    eventPublisher.publishEvent(event);
                    log.debug("{\"message\":\"order_completed_event_published\", \"order_id\":\"{}\", \"component\":\"order-module\"}", id);
                }
                
                return new OrderDto(
                    saved.id(),
                    saved.items() == null ? List.of() 
                        : saved.items().stream().map(OrderItemEntity::itemId).collect(Collectors.toList()),
                    saved.status(),
                    saved.getCreatedAt()
                );
            }
            return null;
        }
        
        synchronized (inMemoryOrders) {
            for (int i = 0; i < inMemoryOrders.size(); i++) {
                var o = inMemoryOrders.get(i);
                if (o.id().equals(id)) {
                    OrderDto updated = new OrderDto(o.id(), o.items(), status, o.createdAt());
                    inMemoryOrders.set(i, updated);
                    
                    log.info("{\"message\":\"order_status_updated\", \"order_id\":\"{}\", \"status\":\"{}\", \"component\":\"order-module\", \"timestamp\":\"{}\"}", 
                        id, status, Instant.now());
                    
                    if ("COMPLETED".equalsIgnoreCase(status)) {
                        OrderCompletedEvent event = new OrderCompletedEvent(id, status, Instant.now());
                        eventPublisher.publishEvent(event);
                    }
                    
                    return updated;
                }
            }
            return null;
        }
    }

    @Transactional(readOnly = true)
    public java.util.Map<String,Object> listOrdersPaged(java.util.Map<String,String> filters) {
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
        
        if (orderRepository != null) {
            var pageable = org.springframework.data.domain.PageRequest.of(page, size);
            org.springframework.data.domain.Page<OrderEntity> pageRes = orderRepository.findAll(pageable);
            List<OrderDto> items = pageRes.stream()
                .map(e -> new OrderDto(
                    e.id(),
                    e.items() == null ? List.of() 
                        : e.items().stream().map(OrderItemEntity::itemId).collect(Collectors.toList()),
                    e.status(),
                    e.getCreatedAt()
                ))
                .collect(Collectors.toList());
                
            java.util.Map<String,Object> out = new java.util.LinkedHashMap<>();
            out.put("items", items);
            out.put("page", pageRes.getNumber());
            out.put("size", pageRes.getSize());
            out.put("totalElements", pageRes.getTotalElements());
            out.put("totalPages", pageRes.getTotalPages());
            return out;
        }
        
        List<OrderDto> all;
        synchronized (inMemoryOrders) {
            all = List.copyOf(inMemoryOrders);
        }
        long totalElements = all.size();
        int from = Math.min(all.size(), page * size);
        int to = Math.min(all.size(), from + size);
        List<OrderDto> pageItems = all.subList(from, to);
        
        java.util.Map<String,Object> out = new java.util.LinkedHashMap<>();
        out.put("items", pageItems);
        out.put("page", page);
        out.put("size", size);
        out.put("totalElements", totalElements);
        out.put("totalPages", (int) Math.ceil((double) totalElements / size));
        return out;
    }
} 

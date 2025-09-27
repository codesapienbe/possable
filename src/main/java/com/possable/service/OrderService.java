package com.possable.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.possable.controller.OrderController.OrderDto;
import com.possable.model.OrderEntity;
import com.possable.model.OrderItemEntity;
import com.possable.repository.OrderItemRepository;
import com.possable.repository.OrderRepository;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final List<OrderDto> inMemoryOrders = Collections.synchronizedList(new ArrayList<>());
    private final TaskExecutor taskExecutor;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Autowired
    public OrderService(TaskExecutor taskExecutor, OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.taskExecutor = taskExecutor;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    // overloaded constructor used by tests to run without repositories (in-memory mode)
    public OrderService(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
        this.orderRepository = null;
        this.orderItemRepository = null;
    }

    // no-arg constructor for AOT/bootstrap scenarios: uses a simple async executor and in-memory repositories
    public OrderService() {
        this.taskExecutor = new SimpleAsyncTaskExecutor();
        this.orderRepository = null;
        this.orderItemRepository = null;
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
            // add items
            for (String itemId : items) {
                OrderItemEntity oi = new OrderItemEntity();
                oi.setItemId(itemId);
                oi.setQuantity(1);
                order.addItem(oi);
            }
            OrderEntity saved = orderRepository.save(order);
            log.info("{\"message\":\"order_created\", \"order_id\":\"{}\", \"items_count\":{}, \"component\":\"order-service\"}", saved.getId(), items.size());
            // async processing
            OrderDto dto = new OrderDto(saved.getId(), saved.getItems().stream().map(i -> i.getItemId()).collect(Collectors.toList()), saved.getStatus(), saved.getCreatedAt());
            CompletableFuture.runAsync(() -> processOrder(dto, notes), taskExecutor::execute).exceptionally(ex -> { log.error("order processing failed for {}", dto.getId(), ex); return null; });
            return dto;
        }

        OrderDto order = new OrderDto(id, List.copyOf(items), "PENDING", now);
        inMemoryOrders.add(order);

        log.info("{\"message\":\"order_created\", \"order_id\":\"{}\", \"items_count\":{}, \"component\":\"order-service\"}", id, items.size());
        CompletableFuture.runAsync(() -> processOrder(order, notes), taskExecutor::execute).exceptionally(ex -> { log.error("order processing failed for {}", id, ex); return null; });
        return order;
    }

    public List<OrderDto> listOrders() {
        if (orderRepository != null) {
            List<OrderDto> out = new ArrayList<>();
            for (OrderEntity e : orderRepository.findAll()) {
                List<String> itemIds = e.getItems() == null ? List.of() : e.getItems().stream().map(i -> i.getItemId()).collect(Collectors.toList());
                out.add(new OrderDto(e.getId(), itemIds, e.getStatus(), e.getCreatedAt()));
            }
            return out;
        }
        synchronized (inMemoryOrders) {
            return List.copyOf(inMemoryOrders);
        }
    }

    public java.util.Map<String,Object> listOrdersPaged(java.util.Map<String,String> filters) {
        int page = 0;
        int size = 20;
        if (filters != null) {
            try { if (filters.containsKey("page")) page = Math.max(0, Integer.parseInt(filters.get("page"))); } catch (Exception ignored) {}
            try { if (filters.containsKey("limit")) size = Math.max(1, Math.min(100, Integer.parseInt(filters.get("limit")))); } catch (Exception ignored) {}
        }
        if (orderRepository != null) {
            var pageable = org.springframework.data.domain.PageRequest.of(page, size);
            org.springframework.data.domain.Page<OrderEntity> pageRes = orderRepository.findAll(pageable);
            List<OrderDto> items = pageRes.stream().map(e -> new OrderDto(e.getId(), e.getItems() == null ? List.of() : e.getItems().stream().map(i -> i.getItemId()).collect(Collectors.toList()), e.getStatus(), e.getCreatedAt())).collect(Collectors.toList());
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

    public OrderDto findById(String id) {
        if (orderRepository != null) {
            return orderRepository.findById(id).map(e -> new OrderDto(e.getId(), e.getItems() == null ? List.of() : e.getItems().stream().map(i -> i.getItemId()).collect(Collectors.toList()), e.getStatus(), e.getCreatedAt())).orElse(null);
        }
        synchronized (inMemoryOrders) {
            return inMemoryOrders.stream().filter(o -> o.getId().equals(id)).findFirst().orElse(null);
        }
    }

    @Transactional
    public OrderDto updateStatus(String id, String status) {
        if (orderRepository != null) {
            var opt = orderRepository.findById(id);
            if (opt.isPresent()) {
                OrderEntity e = opt.get();
                e.setStatus(status);
                OrderEntity saved = orderRepository.save(e);
                log.info("{\"message\":\"order_status_updated\", \"order_id\":\"{}\", \"status\":\"{}\", \"component\":\"order-service\"}", id, status);
                return new OrderDto(saved.getId(), saved.getItems() == null ? List.of() : saved.getItems().stream().map(i -> i.getItemId()).collect(Collectors.toList()), saved.getStatus(), saved.getCreatedAt());
            }
            return null;
        }
        synchronized (inMemoryOrders) {
            for (int i = 0; i < inMemoryOrders.size(); i++) {
                var o = inMemoryOrders.get(i);
                if (o.getId().equals(id)) {
                    OrderDto updated = new OrderDto(o.getId(), o.getItems(), status, o.getCreatedAt());
                    inMemoryOrders.set(i, updated);
                    log.info("{\"message\":\"order_status_updated\", \"order_id\":\"{}\", \"status\":\"{}\", \"component\":\"order-service\"}", id, status);
                    return updated;
                }
            }
            return null;
        }
    }

    private void processOrder(OrderDto order, String notes) {
        log.debug("processing order {}", order.getId());
        try {
            log.info("{\"message\":\"order_processed\", \"order_id\":\"{}\", \"component\":\"order-service\"}", order.getId());
        } catch (Exception e) {
            log.warn("order processing interrupted {}", order.getId());
        }
    }
} 
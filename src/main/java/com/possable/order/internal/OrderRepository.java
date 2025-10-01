package com.possable.order.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Order repository - INTERNAL to order module.
 * Only order module can write to orders table.
 */
interface OrderRepository extends JpaRepository<OrderEntity, String> {
    Page<OrderEntity> findAll(Pageable pageable);
} 
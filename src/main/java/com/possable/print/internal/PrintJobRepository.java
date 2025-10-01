package com.possable.print.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Print job repository - INTERNAL to print module.
 */
interface PrintJobRepository extends JpaRepository<PrintJobEntity, String> {
    Page<PrintJobEntity> findByOrderId(String orderId, Pageable pageable);
    Page<PrintJobEntity> findByStatus(String status, Pageable pageable);
    Page<PrintJobEntity> findByOrderIdAndStatus(String orderId, String status, Pageable pageable);
} 
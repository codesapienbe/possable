package com.possable.print.internal;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Printer repository - INTERNAL to print module.
 */
interface PrinterRepository extends JpaRepository<PrinterEntity, String> {
    List<PrinterEntity> findByCategory(String category);
    Page<PrinterEntity> findAll(Pageable pageable);
} 
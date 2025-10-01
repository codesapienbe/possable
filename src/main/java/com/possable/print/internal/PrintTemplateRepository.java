package com.possable.print.internal;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Print template repository - INTERNAL to print module.
 */
interface PrintTemplateRepository extends JpaRepository<PrintTemplateEntity, String> {
    List<PrintTemplateEntity> findByPrinterCategory(String printerCategory);
    Page<PrintTemplateEntity> findAll(Pageable pageable);
} 
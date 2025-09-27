package com.possable.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.possable.model.PrintTemplateEntity;

@Repository
public interface PrintTemplateRepository extends JpaRepository<PrintTemplateEntity, String> {
	List<PrintTemplateEntity> findByPrinterCategory(String printerCategory);
} 
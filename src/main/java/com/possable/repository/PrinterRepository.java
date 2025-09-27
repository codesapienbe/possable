package com.possable.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.possable.model.PrinterEntity;

@Repository
public interface PrinterRepository extends JpaRepository<PrinterEntity, String> {
} 
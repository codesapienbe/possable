package com.possable.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.possable.model.ItemEntity;

@Repository
public interface ItemRepository extends JpaRepository<ItemEntity, String> {
} 
package com.possable.user.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import com.possable.user.model.ApiKeyEntity;

import java.util.List;

public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, String> {
    List<ApiKeyEntity> findByUsernameAndRevokedFalse(String username);
} 
package com.possable.user.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import com.possable.model.ApiKeyEntity;

public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, String> {
    List<ApiKeyEntity> findByUsernameAndRevokedFalse(String username);
} 
package com.possable.user.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import com.possable.model.UserCredentialEntity;

public interface UserCredentialRepository extends JpaRepository<UserCredentialEntity, String> {
} 
package com.possable.user.internal;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCredentialRepository extends JpaRepository<UserCredentialEntity, String> {
} 
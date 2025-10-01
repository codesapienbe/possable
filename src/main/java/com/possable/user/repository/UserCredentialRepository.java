package com.possable.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.possable.user.model.UserCredentialEntity;

public interface UserCredentialRepository extends JpaRepository<UserCredentialEntity, String> {
} 
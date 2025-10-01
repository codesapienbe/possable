package com.possable.user.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import com.possable.user.model.UserProfileEntity;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, String> {
} 
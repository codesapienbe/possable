package com.possable.user.internal;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.possable.user.model.UserRoleEntity;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, java.io.Serializable> {
    List<UserRoleEntity> findByUsername(String username);
    long countByUsername(String username);
    default void deleteByUsername(String username) {
        findByUsername(username).forEach(r -> delete(r));
    }
} 
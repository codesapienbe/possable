package com.possable.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_role")
@IdClass(UserRoleId.class)
public class UserRoleEntity {

	@Id
	@Column(name = "username", length = 200)
	private String username;

	@Id
	@Column(name = "role", length = 100)
	private String role;

	public UserRoleEntity() {}

	public UserRoleEntity(String username, String role) {
		this.username = username;
		this.role = role;
	}

	public String getUsername() { return username; }
	public void setUsername(String username) { this.username = username; }

	public String getRole() { return role; }
	public void setRole(String role) { this.role = role; }
} 
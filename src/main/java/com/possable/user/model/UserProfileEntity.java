package com.possable.user.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_profile")
public class UserProfileEntity {

	@Id
	@Column(name = "username", nullable = false, length = 200)
	private String username;

	@Column(name = "display_name")
	private String displayName;

	@Column(name = "email")
	private String email;

	@Column(name = "phone")
	private String phone;

	@Lob
	@Column(name = "avatar_bytes")
	private byte[] avatarBytes;

	@Column(name = "avatar_content_type")
	private String avatarContentType;

	@Column(name = "created_at")
	private Instant createdAt;

	public UserProfileEntity() {}

	public UserProfileEntity(String username, String displayName, String email, String phone, byte[] avatarBytes, String avatarContentType, Instant createdAt) {
		this.username = username;
		this.displayName = displayName;
		this.email = email;
		this.phone = phone;
		this.avatarBytes = avatarBytes;
		this.avatarContentType = avatarContentType;
		this.createdAt = createdAt;
	}

	public String getUsername() { return username; }
	public void setUsername(String username) { this.username = username; }

	public String getDisplayName() { return displayName; }
	public void setDisplayName(String displayName) { this.displayName = displayName; }

	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	public String getPhone() { return phone; }
	public void setPhone(String phone) { this.phone = phone; }

	public byte[] getAvatarBytes() { return avatarBytes; }
	public void setAvatarBytes(byte[] avatarBytes) { this.avatarBytes = avatarBytes; }

	public String getAvatarContentType() { return avatarContentType; }
	public void setAvatarContentType(String avatarContentType) { this.avatarContentType = avatarContentType; }

	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
} 
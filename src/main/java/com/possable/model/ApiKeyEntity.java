package com.possable.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "api_key")
public class ApiKeyEntity {

	@Id
	@Column(name = "key_id", nullable = false, length = 64)
	private String keyId;

	@Column(name = "username", nullable = false, length = 200)
	private String username;

	@Column(name = "hashed_token", nullable = false)
	private String hashedToken;

	@Column(name = "label")
	private String label;

	@Column(name = "created_at")
	private Instant createdAt;

	@Column(name = "revoked")
	private boolean revoked;

	public ApiKeyEntity() {}

	public ApiKeyEntity(String keyId, String username, String hashedToken, String label, Instant createdAt, boolean revoked) {
		this.keyId = keyId;
		this.username = username;
		this.hashedToken = hashedToken;
		this.label = label;
		this.createdAt = createdAt;
		this.revoked = revoked;
	}

	public String getKeyId() { return keyId; }
	public void setKeyId(String keyId) { this.keyId = keyId; }

	public String getUsername() { return username; }
	public void setUsername(String username) { this.username = username; }

	public String getHashedToken() { return hashedToken; }
	public void setHashedToken(String hashedToken) { this.hashedToken = hashedToken; }

	public String getLabel() { return label; }
	public void setLabel(String label) { this.label = label; }

	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

	public boolean isRevoked() { return revoked; }
	public void setRevoked(boolean revoked) { this.revoked = revoked; }
} 
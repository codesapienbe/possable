package com.possable.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_credential")
public class UserCredentialEntity {

	@Id
	@Column(name = "username", nullable = false, length = 200)
	private String username;

	@Column(name = "pincode_hash")
	private String pincodeHash;

	@Column(name = "drawing_hash")
	private String drawingHash;

	public UserCredentialEntity() {}

	public UserCredentialEntity(String username, String pincodeHash, String drawingHash) {
		this.username = username;
		this.pincodeHash = pincodeHash;
		this.drawingHash = drawingHash;
	}

	public String getUsername() { return username; }
	public void setUsername(String username) { this.username = username; }

	public String getPincodeHash() { return pincodeHash; }
	public void setPincodeHash(String pincodeHash) { this.pincodeHash = pincodeHash; }

	public String getDrawingHash() { return drawingHash; }
	public void setDrawingHash(String drawingHash) { this.drawingHash = drawingHash; }
} 
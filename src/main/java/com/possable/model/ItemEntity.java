package com.possable.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "items")
public class ItemEntity {
	@Id
	private String id;

	private String name;

	@Column(length = 2000)
	private String description;

	private BigDecimal price;

	private boolean available;

	private Instant createdAt;

	public ItemEntity() {
	}

	public ItemEntity(String id, String name, String description, BigDecimal price, boolean available, Instant createdAt) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.price = price;
		this.available = available;
		this.createdAt = createdAt;
	}

	@PrePersist
	public void prePersist() {
		if (this.id == null) this.id = UUID.randomUUID().toString();
		if (this.createdAt == null) this.createdAt = Instant.now();
	}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }

	public BigDecimal getPrice() { return price; }
	public void setPrice(BigDecimal price) { this.price = price; }

	public boolean isAvailable() { return available; }
	public void setAvailable(boolean available) { this.available = available; }

	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
} 
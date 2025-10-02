package com.possable.inventory.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Item entity - OWNED by inventory module.
 * Other modules can READ but only inventory module can WRITE.
 */
@Entity
@Table(name = "items")
class ItemEntity {
    @Id
    private String id;

    private String name;

    @Column(length = 2000)
    private String description;

    private BigDecimal price;

    private boolean available;

    private Instant createdAt;

    // New fields to support menu metadata
    @Column(length = 128)
    private String category;

    @Column(length = 2000)
    private String tagsCsv; // comma-separated tags

    public ItemEntity() {}

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
    public String id() { return getId(); }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String name() { return getName(); }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String description() { return getDescription(); }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal price() { return getPrice(); }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public boolean available() { return isAvailable(); }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant createdAt() { return getCreatedAt(); }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String category() { return getCategory(); }

    public String getTagsCsv() { return tagsCsv; }
    public void setTagsCsv(String tagsCsv) { this.tagsCsv = tagsCsv; }
    public String tagsCsv() { return getTagsCsv(); }
} 
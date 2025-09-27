package com.possable.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItemEntity {
	@Id
	private String id;

	@ManyToOne
	@JoinColumn(name = "order_id")
	private OrderEntity order;

	@Column(name = "item_id")
	private String itemId;

	private int quantity = 1;

	public OrderItemEntity() {}

	public OrderItemEntity(String id, String itemId, int quantity) {
		this.id = id;
		this.itemId = itemId;
		this.quantity = quantity;
	}

	@PrePersist
	public void prePersist() {
		if (this.id == null) this.id = UUID.randomUUID().toString();
	}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public OrderEntity getOrder() { return order; }
	public void setOrder(OrderEntity order) { this.order = order; }

	public String getItemId() { return itemId; }
	public void setItemId(String itemId) { this.itemId = itemId; }

	public int getQuantity() { return quantity; }
	public void setQuantity(int quantity) { this.quantity = quantity; }
} 
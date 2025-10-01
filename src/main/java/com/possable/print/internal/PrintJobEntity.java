package com.possable.print.internal;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Print job entity - OWNED by print module.
 * References orders via orderId.
 */
@Entity
@Table(name = "print_jobs")
class PrintJobEntity {
    @Id
    private String id;

    @Column(name = "order_id")
    private String orderId;  // Reference to order module

    @Column(name = "printer_id")
    private String printerId;

    @Column(name = "template_id")
    private String templateId;

    @Column(length = 64)
    private String status;

    private Instant createdAt;

    public PrintJobEntity() {}

    public PrintJobEntity(String id, String orderId, String printerId, String templateId, String status, Instant createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.printerId = printerId;
        this.templateId = templateId;
        this.status = status;
        this.createdAt = createdAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.createdAt == null) this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getPrinterId() { return printerId; }
    public void setPrinterId(String printerId) { this.printerId = printerId; }

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
} 
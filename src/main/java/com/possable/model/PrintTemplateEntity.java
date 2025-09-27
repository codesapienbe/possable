package com.possable.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "print_templates")
public class PrintTemplateEntity {
	@Id
	private String id;

	@Column(length = 128)
	private String printerCategory;

	@Column(length = 256)
	private String templateName;

	@Column(length = 4000)
	private String content;

	private Instant createdAt;

	public PrintTemplateEntity() {}

	public PrintTemplateEntity(String id, String printerCategory, String templateName, String content, Instant createdAt) {
		this.id = id;
		this.printerCategory = printerCategory;
		this.templateName = templateName;
		this.content = content;
		this.createdAt = createdAt;
	}

	@PrePersist
	public void prePersist() {
		if (this.id == null) this.id = UUID.randomUUID().toString();
		if (this.createdAt == null) this.createdAt = Instant.now();
	}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public String getPrinterCategory() { return printerCategory; }
	public void setPrinterCategory(String printerCategory) { this.printerCategory = printerCategory; }

	public String getTemplateName() { return templateName; }
	public void setTemplateName(String templateName) { this.templateName = templateName; }

	public String getContent() { return content; }
	public void setContent(String content) { this.content = content; }

	public java.time.Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }
} 
package com.yusdev.procuraduriaprep.documents.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Lob;

@Entity
@Table(name = "study_documents")
public class StudyDocument {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "original_name", nullable = false, length = 255)
	private String originalName;

	@Column(name = "content_type", nullable = false, length = 150)
	private String contentType;

	@Column(name = "storage_reference", nullable = false, length = 1000)
	private String storageReference;

	@Lob
	@Column(name = "extracted_text")
	private String extractedText;

	@Column(name = "processing_error", length = 500)
	private String processingError;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DocumentStatus status;

	@Column(name = "uploaded_at", nullable = false, updatable = false)
	private Instant uploadedAt;

	protected StudyDocument() {
	}

	public StudyDocument(String originalName, String contentType, String storageReference) {
		this.originalName = originalName;
		this.contentType = contentType;
		this.storageReference = storageReference;
		this.status = DocumentStatus.PENDING;
		this.uploadedAt = Instant.now();
	}

	public void deactivate() {
		this.status = DocumentStatus.INACTIVE;
	}

	public void completeProcessing(String extractedText) {
		this.extractedText = extractedText;
		this.processingError = null;
		this.status = DocumentStatus.COMPLETED;
	}

	public void failProcessing(String processingError) {
		this.processingError = processingError;
		this.status = DocumentStatus.FAILED;
	}

	public boolean isActive() {
		return status != DocumentStatus.INACTIVE;
	}

	public UUID getId() {
		return id;
	}

	public String getOriginalName() {
		return originalName;
	}

	public String getContentType() {
		return contentType;
	}

	public String getStorageReference() {
		return storageReference;
	}

	public String getExtractedText() {
		return extractedText;
	}

	public String getProcessingError() {
		return processingError;
	}

	public DocumentStatus getStatus() {
		return status;
	}

	public Instant getUploadedAt() {
		return uploadedAt;
	}
}

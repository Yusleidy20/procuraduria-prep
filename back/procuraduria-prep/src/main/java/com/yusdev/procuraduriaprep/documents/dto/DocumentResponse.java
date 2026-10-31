package com.yusdev.procuraduriaprep.documents.dto;

import java.time.Instant;
import java.util.UUID;

import com.yusdev.procuraduriaprep.documents.entity.DocumentStatus;
import com.yusdev.procuraduriaprep.documents.entity.StudyDocument;

public record DocumentResponse(
		UUID id,
		String originalName,
		String contentType,
		String storageReference,
		DocumentStatus status,
		String processingError,
		Instant uploadedAt) {

	public static DocumentResponse from(StudyDocument document) {
		return new DocumentResponse(document.getId(), document.getOriginalName(), document.getContentType(),
				document.getStorageReference(), document.getStatus(), document.getProcessingError(),
				document.getUploadedAt());
	}
}

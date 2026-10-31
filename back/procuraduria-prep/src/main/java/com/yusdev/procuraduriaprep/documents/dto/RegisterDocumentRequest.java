package com.yusdev.procuraduriaprep.documents.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDocumentRequest(
		@NotBlank @Size(max = 255) String originalName,
		@NotBlank @Size(max = 150) String contentType,
		@NotBlank @Size(max = 1000) String storageReference) {
}

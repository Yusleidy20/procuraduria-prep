package com.yusdev.procuraduriaprep.documents.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.yusdev.procuraduriaprep.documents.dto.DocumentResponse;
import com.yusdev.procuraduriaprep.documents.dto.DocumentTextResponse;
import com.yusdev.procuraduriaprep.documents.dto.RegisterDocumentRequest;
import com.yusdev.procuraduriaprep.documents.service.DocumentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

	private final DocumentService documentService;

	public DocumentController(DocumentService documentService) {
		this.documentService = documentService;
	}

	@PostMapping
	public ResponseEntity<DocumentResponse> register(
			@Valid @RequestBody RegisterDocumentRequest request,
			UriComponentsBuilder uriBuilder) {
		DocumentResponse response = documentService.register(request);
		URI location = uriBuilder.path("/api/v1/documents/{id}").buildAndExpand(response.id()).toUri();
		return ResponseEntity.created(location).body(response);
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<DocumentResponse> upload(
			@RequestPart("file") MultipartFile file,
			UriComponentsBuilder uriBuilder) {
		DocumentResponse response = documentService.upload(file);
		URI location = uriBuilder.path("/api/v1/documents/{id}").buildAndExpand(response.id()).toUri();
		return ResponseEntity.created(location).body(response);
	}

	@GetMapping
	public Page<DocumentResponse> getDocuments(@PageableDefault(size = 20, sort = "uploadedAt") Pageable pageable) {
		return documentService.getDocuments(pageable);
	}

	@GetMapping("/{documentId}/status")
	public DocumentResponse getStatus(@PathVariable UUID documentId) {
		return documentService.getStatus(documentId);
	}

	@GetMapping("/{documentId}")
	public DocumentResponse getDocument(@PathVariable UUID documentId) {
		return documentService.getStatus(documentId);
	}

	@GetMapping("/{documentId}/text")
	public DocumentTextResponse getText(@PathVariable UUID documentId) {
		return documentService.getText(documentId);
	}

	@DeleteMapping("/{documentId}")
	public ResponseEntity<Void> deactivate(@PathVariable UUID documentId) {
		documentService.deactivate(documentId);
		return ResponseEntity.noContent().build();
	}
}

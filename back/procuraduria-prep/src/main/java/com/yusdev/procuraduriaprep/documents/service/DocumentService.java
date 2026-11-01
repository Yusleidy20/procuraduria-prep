package com.yusdev.procuraduriaprep.documents.service;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.yusdev.procuraduriaprep.documents.dto.DocumentTextResponse;
import com.yusdev.procuraduriaprep.documents.dto.DocumentResponse;
import com.yusdev.procuraduriaprep.documents.dto.RegisterDocumentRequest;
import com.yusdev.procuraduriaprep.documents.entity.DocumentStatus;
import com.yusdev.procuraduriaprep.documents.entity.StudyDocument;
import com.yusdev.procuraduriaprep.documents.repository.StudyDocumentRepository;
import com.yusdev.procuraduriaprep.documents.repository.DocumentChunkRepository;
import com.yusdev.procuraduriaprep.shared.exception.InvalidRequestException;
import com.yusdev.procuraduriaprep.shared.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class DocumentService {

	private final StudyDocumentRepository documentRepository;
	private final DocumentTextExtractor textExtractor;
	private final DocumentStorageService storageService;
	private final DocumentChunkingService chunkingService;
	private final DocumentChunkRepository chunkRepository;
	private final long maximumFileSize;

	public DocumentService(StudyDocumentRepository documentRepository, DocumentTextExtractor textExtractor,
			DocumentStorageService storageService, DocumentChunkingService chunkingService,
			DocumentChunkRepository chunkRepository,
			@Value("${app.documents.max-file-size-bytes:10485760}") long maximumFileSize) {
		this.documentRepository = documentRepository;
		this.textExtractor = textExtractor;
		this.storageService = storageService;
		this.chunkingService = chunkingService;
		this.chunkRepository = chunkRepository;
		this.maximumFileSize = maximumFileSize;
	}

	@Transactional
	public DocumentResponse register(RegisterDocumentRequest request) {
		StudyDocument document = documentRepository.save(new StudyDocument(request.originalName().trim(),
				request.contentType().trim(), request.storageReference().trim()));
		return DocumentResponse.from(document);
	}

	@Transactional
	public DocumentResponse upload(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new InvalidRequestException("Debe adjuntar un archivo no vacío");
		}
		if (file.getSize() > maximumFileSize) {
			throw new InvalidRequestException("El documento supera el límite de tamaño permitido");
		}
		String originalName = sanitizeFileName(file.getOriginalFilename());
		String extension = extensionOf(originalName);
		String expectedContentType = contentTypeFor(extension);
		String contentType = file.getContentType();
		if (!isCompatibleContentType(extension, contentType)) {
			throw new InvalidRequestException("La extensión y el tipo de contenido del archivo no coinciden");
		}

		byte[] content;
		try {
			content = file.getBytes();
		} catch (IOException exception) {
			throw new InvalidRequestException("No fue posible leer el archivo enviado");
		}
		var pages = textExtractor.extractPages(expectedContentType, content);
		String extractedText = pages.stream().map(DocumentTextExtractor.ExtractedPage::text)
				.collect(java.util.stream.Collectors.joining("\n"));
		UUID documentId = UUID.randomUUID();
		String storageReference = storageService.store(documentId, extension, content);
		StudyDocument document = new StudyDocument(originalName, expectedContentType, storageReference);
		document.completeProcessing(extractedText);
		try {
			StudyDocument savedDocument = documentRepository.save(document);
			chunkingService.index(savedDocument, pages);
			return DocumentResponse.from(savedDocument);
		} catch (RuntimeException exception) {
			try {
				storageService.delete(storageReference);
			} catch (RuntimeException cleanupException) {
				exception.addSuppressed(cleanupException);
			}
			throw exception;
		}
	}

	public Page<DocumentResponse> getDocuments(Pageable pageable) {
		return documentRepository.findAllByStatusNot(DocumentStatus.INACTIVE, pageable).map(DocumentResponse::from);
	}

	public DocumentResponse getStatus(UUID documentId) {
		StudyDocument document = documentRepository.findById(documentId)
				.orElseThrow(() -> new ResourceNotFoundException("Documento", documentId));
		return DocumentResponse.from(document);
	}

	public DocumentTextResponse getText(UUID documentId) {
		StudyDocument document = documentRepository.findById(documentId)
				.orElseThrow(() -> new ResourceNotFoundException("Documento", documentId));
		if (document.getStatus() != DocumentStatus.COMPLETED) {
			throw new InvalidRequestException("El documento aún no tiene texto procesado");
		}
		return new DocumentTextResponse(document.getId(), document.getOriginalName(), document.getExtractedText());
	}

	@Transactional
	public void deactivate(UUID documentId) {
		StudyDocument document = documentRepository.findById(documentId)
				.orElseThrow(() -> new ResourceNotFoundException("Documento", documentId));
		chunkRepository.deleteAllByDocument_Id(documentId);
		document.deactivate();
	}

	private String sanitizeFileName(String fileName) {
		if (fileName == null || fileName.isBlank()) {
			throw new InvalidRequestException("El archivo debe tener un nombre válido");
		}
		String sanitized = fileName.replace('\\', '/');
		sanitized = sanitized.substring(sanitized.lastIndexOf('/') + 1).trim();
		if (sanitized.isBlank() || sanitized.length() > 255 || sanitized.equals(".") || sanitized.equals("..")) {
			throw new InvalidRequestException("El nombre del archivo no es válido");
		}
		return sanitized;
	}

	private String extensionOf(String fileName) {
		int dot = fileName.lastIndexOf('.');
		if (dot < 1) {
			throw new InvalidRequestException("Solo se admiten archivos PDF, TXT o Markdown");
		}
		String extension = fileName.substring(dot).toLowerCase(Locale.ROOT);
		if (!extension.equals(".pdf") && !extension.equals(".txt") && !extension.equals(".md")) {
			throw new InvalidRequestException("Solo se admiten archivos PDF, TXT o Markdown");
		}
		return extension;
	}

	private boolean isCompatibleContentType(String extension, String contentType) {
		if (contentType == null || contentType.isBlank()
				|| "application/octet-stream".equalsIgnoreCase(contentType)) {
			return true;
		}
		String normalizedType = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
		return switch (extension) {
			case ".pdf" -> normalizedType.equals("application/pdf")
					|| normalizedType.equals("application/x-pdf")
					|| normalizedType.equals("application/acrobat")
					|| normalizedType.equals("application/vnd.pdf")
					|| normalizedType.equals("text/pdf");
			case ".txt" -> normalizedType.equals("text/plain");
			case ".md" -> normalizedType.equals("text/markdown")
					|| normalizedType.equals("text/x-markdown")
					|| normalizedType.equals("text/plain");
			default -> false;
		};
	}

	private String contentTypeFor(String extension) {
		return switch (extension) {
			case ".pdf" -> "application/pdf";
			case ".txt" -> "text/plain";
			case ".md" -> "text/markdown";
			default -> throw new InvalidRequestException("Formato de archivo no compatible");
		};
	}
}

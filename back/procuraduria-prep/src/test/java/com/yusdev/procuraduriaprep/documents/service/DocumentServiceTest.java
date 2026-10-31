package com.yusdev.procuraduriaprep.documents.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.yusdev.procuraduriaprep.documents.dto.DocumentTextResponse;
import com.yusdev.procuraduriaprep.documents.dto.RegisterDocumentRequest;
import com.yusdev.procuraduriaprep.documents.entity.DocumentStatus;
import com.yusdev.procuraduriaprep.documents.entity.StudyDocument;
import com.yusdev.procuraduriaprep.documents.repository.StudyDocumentRepository;
import com.yusdev.procuraduriaprep.documents.repository.DocumentChunkRepository;
import com.yusdev.procuraduriaprep.shared.exception.InvalidRequestException;
import com.yusdev.procuraduriaprep.shared.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

	@Mock
	private StudyDocumentRepository documentRepository;

	@Mock
	private DocumentTextExtractor textExtractor;

	@Mock
	private DocumentStorageService storageService;

	@Mock
	private DocumentChunkingService chunkingService;

	@Mock
	private DocumentChunkRepository chunkRepository;

	private DocumentService documentService;

	@BeforeEach
	void setUp() {
		documentService = new DocumentService(documentRepository, textExtractor, storageService, chunkingService,
				chunkRepository, 1_000_000);
	}

	@Test
	void registersDocumentAsPending() {
		when(documentRepository.save(org.mockito.ArgumentMatchers.any(StudyDocument.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		var response = documentService.register(
				new RegisterDocumentRequest("ley.pdf", "application/pdf", "local://ley.pdf"));

		assertEquals(DocumentStatus.PENDING, response.status());
		assertEquals("ley.pdf", response.originalName());
	}

	@Test
	void deactivatesDocumentWithoutDeletingIt() {
		UUID id = UUID.randomUUID();
		StudyDocument document = new StudyDocument("ley.pdf", "application/pdf", "local://ley.pdf");
		when(documentRepository.findById(id)).thenReturn(Optional.of(document));

		documentService.deactivate(id);

		assertEquals(DocumentStatus.INACTIVE, document.getStatus());
		verify(documentRepository).findById(id);
		verify(chunkRepository).deleteAllByDocument_Id(id);
	}

	@Test
	void rejectsMissingDocument() {
		UUID id = UUID.randomUUID();
		when(documentRepository.findById(id)).thenReturn(Optional.empty());

		org.junit.jupiter.api.Assertions.assertThrows(ResourceNotFoundException.class,
				() -> documentService.deactivate(id));
	}

	@Test
	void uploadsAndProcessesUtf8TextWithoutExposingFilesystemPath() throws Exception {
		byte[] content = "Constitución y derechos fundamentales".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		MockMultipartFile file = new MockMultipartFile("file", "../../ley.txt", "text/plain", content);
		var pages = java.util.List.of(new DocumentTextExtractor.ExtractedPage(null,
				"Constitución y derechos fundamentales"));
		when(textExtractor.extractPages("text/plain", content)).thenReturn(pages);
		when(storageService.store(any(), org.mockito.ArgumentMatchers.eq(".txt"), any()))
				.thenReturn("local://documents/document.txt");
		when(documentRepository.save(any(StudyDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var response = documentService.upload(file);

		assertEquals("ley.txt", response.originalName());
		assertEquals(DocumentStatus.COMPLETED, response.status());
		assertEquals("local://documents/document.txt", response.storageReference());
		verify(storageService).store(any(), org.mockito.ArgumentMatchers.eq(".txt"), any());
		verify(chunkingService).index(any(StudyDocument.class), org.mockito.ArgumentMatchers.eq(pages));
	}

	@Test
	void rejectsDisallowedExtensionsAndDoesNotStoreFile() {
		MockMultipartFile file = new MockMultipartFile("file", "script.exe", "application/octet-stream",
				new byte[] { 1, 2, 3 });

		assertThrows(InvalidRequestException.class, () -> documentService.upload(file));
		verify(storageService, never()).store(any(), any(), any());
		verify(documentRepository, never()).save(any());
	}

	@Test
	void returnsExtractedTextForCompletedDocument() {
		UUID id = UUID.randomUUID();
		StudyDocument document = new StudyDocument("ley.txt", "text/plain", "local://documents/ley.txt");
		document.completeProcessing("Contenido del documento");
		when(documentRepository.findById(id)).thenReturn(Optional.of(document));

		DocumentTextResponse response = documentService.getText(id);

		assertEquals("Contenido del documento", response.text());
	}
}

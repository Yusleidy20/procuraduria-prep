package com.yusdev.procuraduriaprep.documents.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.yusdev.procuraduriaprep.documents.entity.DocumentStatus;
import com.yusdev.procuraduriaprep.documents.repository.DocumentChunkRepository;

@SpringBootTest(properties = "app.documents.storage-dir=${java.io.tmpdir}/procuraduria-prep-document-test")
@ActiveProfiles("test")
@Transactional
class DocumentUploadIntegrationTest {

	@Autowired
	private DocumentService documentService;

	@Autowired
	private DocumentStorageService storageService;

	@Autowired
	private DocumentChunkRepository chunkRepository;

	@Test
	void persistsUploadedContentAndReturnsExtractedText() {
		byte[] content = "Artículo de prueba UTF-8".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		MockMultipartFile file = new MockMultipartFile("file", "ley.txt", "text/plain", content);
		var uploaded = documentService.upload(file);

		try {
			assertEquals(DocumentStatus.COMPLETED, uploaded.status());
			assertEquals("Artículo de prueba UTF-8", documentService.getText(uploaded.id()).text());
			assertTrue(uploaded.storageReference().startsWith("local://documents/"));
		} finally {
			storageService.delete(uploaded.storageReference());
		}
	}

	@Test
	void persistsChunksAndRemovesThemWhenDocumentIsDeactivated() {
		byte[] content = ("Artículo 86. Toda persona tendrá acción de tutela para reclamar ante los jueces, "
				+ "en todo momento y lugar, mediante un procedimiento preferente y sumario.")
				.getBytes(java.nio.charset.StandardCharsets.UTF_8);
		MockMultipartFile file = new MockMultipartFile("file", "constitucion.txt", "text/plain", content);
		var uploaded = documentService.upload(file);

		try {
			var chunks = chunkRepository.findAllByDocument_IdAndDocument_StatusNot(uploaded.id(),
					DocumentStatus.INACTIVE);
			assertEquals(1, chunks.size());
			assertEquals("Artículo 86.", chunks.get(0).getText().substring(0, 12));
			assertTrue(chunks.get(0).getTermVector().contains("86:1"));

			documentService.deactivate(uploaded.id());

			assertTrue(chunkRepository.findAllByDocument_IdAndDocument_StatusNot(uploaded.id(),
					DocumentStatus.INACTIVE).isEmpty());
		} finally {
			storageService.delete(uploaded.storageReference());
		}
	}
}

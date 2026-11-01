package com.yusdev.procuraduriaprep.documents.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.yusdev.procuraduriaprep.documents.entity.DocumentStatus;
import com.yusdev.procuraduriaprep.documents.entity.StudyDocument;
import com.yusdev.procuraduriaprep.documents.repository.DocumentChunkRepository;
import com.yusdev.procuraduriaprep.documents.repository.StudyDocumentRepository;
import com.yusdev.procuraduriaprep.ai.dto.ExplainTopicRequest;
import com.yusdev.procuraduriaprep.ai.service.AiProvider;
import com.yusdev.procuraduriaprep.ai.service.AiTutorContentService;
import com.yusdev.procuraduriaprep.shared.exception.InvalidRequestException;

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

	@Autowired
	private StudyDocumentRepository documentRepository;

	@Autowired
	private AiTutorContentService aiTutorContentService;

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
	void uploadsPdfUsingPdfBoxEvenWhenBrowserReportsAnAlternatePdfMimeType() throws Exception {
		byte[] content = createPdf("Ley 2094 de 2021: responsabilidad disciplinaria");
		MockMultipartFile file = new MockMultipartFile(
				"file", "Ley_2094_de_2021.pdf", "application/x-pdf", content);

		var uploaded = documentService.upload(file);

		try {
			assertEquals("Ley_2094_de_2021.pdf", uploaded.originalName());
			assertEquals("application/pdf", uploaded.contentType());
			assertEquals(DocumentStatus.COMPLETED, uploaded.status());
			assertTrue(documentService.getText(uploaded.id()).text().contains("Ley 2094 de 2021"));
		} finally {
			storageService.delete(uploaded.storageReference());
		}
	}

	@Test
	void uploadsMarkdownAsUtf8Text() {
		byte[] content = "# Ley 2094\n\nArtículo 1: contenido de prueba".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		MockMultipartFile file = new MockMultipartFile("file", "ley.md", "text/markdown", content);
		var uploaded = documentService.upload(file);

		try {
			assertEquals(DocumentStatus.COMPLETED, uploaded.status());
			assertEquals("# Ley 2094\n\nArtículo 1: contenido de prueba",
					documentService.getText(uploaded.id()).text());
		} finally {
			storageService.delete(uploaded.storageReference());
		}
	}

	@Test
	void rejectsEmptyAndUnreadablePdfWithoutStoringThem() {
		MockMultipartFile empty = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
		MockMultipartFile unreadable = new MockMultipartFile(
				"file", "broken.pdf", "application/pdf", "%PDF-1.7\ntruncated".getBytes(java.nio.charset.StandardCharsets.US_ASCII));

		InvalidRequestException emptyError = assertThrows(InvalidRequestException.class, () -> documentService.upload(empty));
		InvalidRequestException unreadableError = assertThrows(InvalidRequestException.class, () -> documentService.upload(unreadable));

		assertTrue(emptyError.getMessage().contains("no vacío"));
		assertTrue(unreadableError.getMessage().contains("dañado o protegido"));
	}

	@Test
	void rejectsNonPdfContentRenamedWithPdfExtension() {
		MockMultipartFile renamedText = new MockMultipartFile(
				"file", "Ley_2094_de_2021.pdf", "application/pdf",
				"Esto es texto, no un PDF".getBytes(java.nio.charset.StandardCharsets.UTF_8));

		InvalidRequestException exception = assertThrows(
				InvalidRequestException.class, () -> documentService.upload(renamedText));

		assertTrue(exception.getMessage().contains("No fue posible leer el PDF"));
	}

	@Test
	void acceptsPdfWithPrefixWhenPdfBoxCanParseIt() throws Exception {
		byte[] pdf = createPdf("Ley 2094 de 2021: responsabilidad disciplinaria");
		byte[] prefix = "document tag".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
		byte[] content = new byte[prefix.length + pdf.length];
		System.arraycopy(prefix, 0, content, 0, prefix.length);
		System.arraycopy(pdf, 0, content, prefix.length, pdf.length);
		MockMultipartFile file = new MockMultipartFile(
				"file", "Ley_2094_de_2021.pdf", "application/pdf", content);

		var uploaded = documentService.upload(file);

		try {
			assertEquals(DocumentStatus.COMPLETED, uploaded.status());
			assertTrue(documentService.getText(uploaded.id()).text().contains("Ley 2094 de 2021"));
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

	@Test
	void queriesWholeLibraryIndexesLegacyTextAndReturnsAnAiExplanationWithRetrievedSource() {
		StudyDocument legacyDocument = new StudyDocument(
				"gestion-publica.txt", "text/plain", "local://documents/legacy");
		legacyDocument.completeProcessing("La gestión pública organiza el funcionamiento del Estado.");
		legacyDocument = documentRepository.saveAndFlush(legacyDocument);
		var legacyDocumentId = legacyDocument.getId();
		var prompt = new java.util.concurrent.atomic.AtomicReference<String>();
		AiProvider testProvider = (providerPrompt, schema) -> {
			prompt.set(providerPrompt);
			return """
					{"explanation":"La fuente describe cómo se organiza la gestión pública [S1].",
					"keyPoints":"Se refiere al funcionamiento del Estado."}
					""";
		};

		var response = aiTutorContentService.explainTopic(
				new ExplainTopicRequest("Gestión pública y funcionamiento del Estado", null, "Inicial", null),
				testProvider);

		assertEquals(1, response.sources().size());
		assertEquals(legacyDocumentId, response.sources().get(0).documentId());
		assertEquals("gestion-publica.txt", response.sources().get(0).documentName());
		assertTrue(prompt.get().contains("La gestión pública organiza el funcionamiento del Estado."));
		assertTrue(response.explanation().contains("La fuente describe"));
		assertEquals(1, chunkRepository.findAllByDocument_IdAndDocument_StatusNot(
				legacyDocumentId, DocumentStatus.INACTIVE).size());

		var documentResponse = aiTutorContentService.explainTopic(
				new ExplainTopicRequest("Gestión pública y funcionamiento del Estado",
						null, "Inicial", legacyDocumentId),
				testProvider);
		assertEquals(legacyDocumentId, documentResponse.sources().get(0).documentId());
	}

	@Test
	void explainsThatTheLibraryIsEmptyWithoutCallingTheAiProvider() {
		AiProvider provider = (prompt, schema) -> {
			throw new AssertionError("The provider should not be called for an empty library");
		};

		var response = aiTutorContentService.explainTopic(
				new ExplainTopicRequest("Gestión pública y funcionamiento del Estado", null, "Inicial", null),
				provider);

		assertTrue(response.explanation().contains("biblioteca aún no tiene documentos"));
		assertTrue(response.sources().isEmpty());
	}

	private byte[] createPdf(String text) throws Exception {
		try (PDDocument document = new PDDocument();
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			PDPage page = new PDPage();
			document.addPage(page);
			try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
				stream.beginText();
				stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
				stream.newLineAtOffset(50, 700);
				stream.showText(text);
				stream.endText();
			}
			document.save(output);
			return output.toByteArray();
		}
	}
}

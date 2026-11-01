package com.yusdev.procuraduriaprep.documents.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import com.yusdev.procuraduriaprep.shared.exception.InvalidRequestException;

class DocumentTextExtractorTest {

	private final DocumentTextExtractor extractor = new DocumentTextExtractor();

	@Test
	void extractsUtf8PlainText() {
		assertEquals("Artículo 1: La Constitución", extractor.extract(
				"text/plain", "Artículo 1: La Constitución".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
	}

	@Test
	void extractsUtf8Markdown() {
		assertEquals("# Ley 2094\n\nArtículo 1", extractor.extract(
				"text/markdown", "# Ley 2094\n\nArtículo 1".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
	}

	@Test
	void rejectsEmptyPlainTextAndMarkdown() {
		InvalidRequestException emptyText = assertThrows(InvalidRequestException.class,
				() -> extractor.extract("text/plain", new byte[0]));
		InvalidRequestException emptyMarkdown = assertThrows(InvalidRequestException.class,
				() -> extractor.extract("text/markdown", " \n\t".getBytes(java.nio.charset.StandardCharsets.UTF_8)));

		assertEquals("El documento no contiene texto", emptyText.getMessage());
		assertEquals("El documento no contiene texto", emptyMarkdown.getMessage());
	}

	@Test
	void extractsPdfText() throws Exception {
		byte[] pdf = createPdfWithText("Public law study text");

		assertTrue(extractor.extract("application/pdf", pdf).contains("Public law study text"));
		assertEquals(1, extractor.extractPages("application/pdf", pdf).get(0).pageNumber());
	}

	@Test
	void rejectsPdfWithoutExtractableTextWithAnOcrGuidanceMessage() throws Exception {
		byte[] pdf = createBlankPdf();

		InvalidRequestException exception = assertThrows(InvalidRequestException.class,
				() -> extractor.extract("application/pdf", pdf));

		assertTrue(exception.getMessage().contains("si es escaneado, requiere OCR"));
	}

	@Test
	void rejectsMalformedPdfAndUnsupportedType() {
		InvalidRequestException malformedPdf = assertThrows(InvalidRequestException.class,
				() -> extractor.extract("application/pdf", new byte[] { 1, 2, 3 }));
		assertThrows(InvalidRequestException.class, () -> extractor.extract("application/zip", new byte[] { 1 }));
		assertTrue(malformedPdf.getMessage().contains("No fue posible leer el PDF"));
	}

	@Test
	void extractsPdfWhenParserAcceptsBytesBeforeTheHeader() throws Exception {
		byte[] pdf = createPdfWithText("Public law study text");
		byte[] prefixedPdf = new byte[pdf.length + 12];
		byte[] prefix = "document tag".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
		System.arraycopy(prefix, 0, prefixedPdf, 0, prefix.length);
		System.arraycopy(pdf, 0, prefixedPdf, prefix.length, pdf.length);

		assertTrue(extractor.extract("application/pdf", prefixedPdf).contains("Public law study text"));
	}

	@Test
	void rejectsTruncatedPdfWithAReadableError() {
		InvalidRequestException exception = assertThrows(InvalidRequestException.class,
				() -> extractor.extract("application/pdf", "%PDF-1.7\ntruncated".getBytes(java.nio.charset.StandardCharsets.US_ASCII)));

		assertTrue(exception.getMessage().contains("dañado o protegido"));
	}

	@Test
	void rejectsInvalidUtf8() {
		assertThrows(InvalidRequestException.class,
				() -> extractor.extract("text/plain", new byte[] { (byte) 0xC3, 0x28 }));
	}

	private byte[] createPdfWithText(String text) throws Exception {
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

	private byte[] createBlankPdf() throws Exception {
		try (PDDocument document = new PDDocument();
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			document.addPage(new PDPage());
			document.save(output);
			return output.toByteArray();
		}
	}
}

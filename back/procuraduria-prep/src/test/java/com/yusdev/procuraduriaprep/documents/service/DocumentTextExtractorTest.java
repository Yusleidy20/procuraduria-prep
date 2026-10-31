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
	void extractsPdfText() throws Exception {
		byte[] pdf;
		try (PDDocument document = new PDDocument();
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			PDPage page = new PDPage();
			document.addPage(page);
			try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
				stream.beginText();
				stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
				stream.newLineAtOffset(50, 700);
				stream.showText("Public law study text");
				stream.endText();
			}
			document.save(output);
			pdf = output.toByteArray();
		}

		assertTrue(extractor.extract("application/pdf", pdf).contains("Public law study text"));
		assertEquals(1, extractor.extractPages("application/pdf", pdf).get(0).pageNumber());
	}

	@Test
	void rejectsMalformedPdfAndUnsupportedType() {
		assertThrows(InvalidRequestException.class, () -> extractor.extract("application/pdf", new byte[] { 1, 2, 3 }));
		assertThrows(InvalidRequestException.class, () -> extractor.extract("application/zip", new byte[] { 1 }));
	}

	@Test
	void rejectsInvalidUtf8() {
		assertThrows(InvalidRequestException.class,
				() -> extractor.extract("text/plain", new byte[] { (byte) 0xC3, 0x28 }));
	}
}

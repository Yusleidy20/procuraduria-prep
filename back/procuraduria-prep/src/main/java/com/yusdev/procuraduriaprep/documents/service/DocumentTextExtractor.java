package com.yusdev.procuraduriaprep.documents.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import com.yusdev.procuraduriaprep.shared.exception.InvalidRequestException;

@Component
public class DocumentTextExtractor {

	public String extract(String contentType, byte[] content) {
		return extractPages(contentType, content).stream()
				.map(ExtractedPage::text)
				.collect(java.util.stream.Collectors.joining("\n"));
	}

	public List<ExtractedPage> extractPages(String contentType, byte[] content) {
		if ("application/pdf".equals(contentType)) {
			List<ExtractedPage> pages = extractPdfPages(content);
			if (pages.isEmpty()) {
				throw new InvalidRequestException("El PDF no contiene páginas legibles");
			}
			if (pages.stream().allMatch(page -> page.text().isBlank())) {
				throw new InvalidRequestException("El PDF no contiene texto extraíble; si es escaneado, requiere OCR");
			}
			return pages;
		}
		if ("text/plain".equals(contentType) || "text/markdown".equals(contentType)) {
			return List.of(new ExtractedPage(null, decodeText(content)));
		}
		throw new InvalidRequestException("Tipo de documento no compatible; use PDF, TXT o Markdown");
	}

	private List<ExtractedPage> extractPdfPages(byte[] content) {
		try (var document = Loader.loadPDF(content)) {
			List<ExtractedPage> pages = new ArrayList<>();
			for (int pageNumber = 1; pageNumber <= document.getNumberOfPages(); pageNumber++) {
				PDFTextStripper stripper = new PDFTextStripper();
				stripper.setStartPage(pageNumber);
				stripper.setEndPage(pageNumber);
				pages.add(new ExtractedPage(pageNumber, stripper.getText(document).strip()));
			}
			return pages;
		} catch (IOException exception) {
			throw new InvalidRequestException("No fue posible leer el PDF; puede estar dañado o protegido con contraseña");
		}

	}

	public record ExtractedPage(Integer pageNumber, String text) {
	}

	private String decodeText(byte[] content) {
		try {
			String text = StandardCharsets.UTF_8.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.decode(ByteBuffer.wrap(content))
					.toString()
					.strip();
			if (text.isBlank()) {
				throw new InvalidRequestException("El documento no contiene texto");
			}
			return text;
		} catch (CharacterCodingException exception) {
			throw new InvalidRequestException("Los archivos de texto deben estar codificados en UTF-8");
		}
	}
}

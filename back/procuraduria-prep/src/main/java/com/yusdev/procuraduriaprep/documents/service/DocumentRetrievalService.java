package com.yusdev.procuraduriaprep.documents.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yusdev.procuraduriaprep.ai.dto.DocumentSourceReference;
import com.yusdev.procuraduriaprep.documents.entity.DocumentChunk;
import com.yusdev.procuraduriaprep.documents.entity.DocumentStatus;
import com.yusdev.procuraduriaprep.documents.entity.StudyDocument;
import com.yusdev.procuraduriaprep.documents.repository.DocumentChunkRepository;
import com.yusdev.procuraduriaprep.documents.repository.StudyDocumentRepository;
import com.yusdev.procuraduriaprep.shared.exception.InvalidRequestException;
import com.yusdev.procuraduriaprep.shared.exception.ResourceNotFoundException;

@Service
public class DocumentRetrievalService {

    private static final int MAX_CHUNKS = 5;
    private static final int MAX_CONTEXT_CHARS = 8500;
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}\\p{N}]+");

    private final DocumentChunkRepository chunkRepository;
    private final StudyDocumentRepository documentRepository;
    private final DocumentChunkingService chunkingService;

    public DocumentRetrievalService(DocumentChunkRepository chunkRepository,
                                    StudyDocumentRepository documentRepository,
                                    DocumentChunkingService chunkingService) {
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
        this.chunkingService = chunkingService;
    }

    @Transactional
    public RetrievedDocumentContext retrieve(String query, UUID documentId) {
        List<DocumentChunk> chunks;
        if (documentId == null) {
            for (StudyDocument document : documentRepository
                    .findCompletedDocumentsWithoutChunks(DocumentStatus.COMPLETED)) {
                chunkingService.index(document, List.of(new DocumentTextExtractor.ExtractedPage(
                        null, document.getExtractedText())));
            }
            chunks = chunkRepository.findAllByDocument_StatusNot(DocumentStatus.INACTIVE);
        } else {
            StudyDocument document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Documento", documentId));
            if (!document.isActive()) {
                throw new ResourceNotFoundException("Documento activo", documentId);
            }
            if (document.getStatus() != DocumentStatus.COMPLETED
                    || document.getExtractedText() == null || document.getExtractedText().isBlank()) {
                throw new InvalidRequestException("El documento aún no tiene texto procesado");
            }
            chunks = chunkRepository.findAllByDocument_IdAndDocument_StatusNot(documentId, DocumentStatus.INACTIVE);
            if (chunks.isEmpty()) {
                chunkingService.index(document, List.of(new DocumentTextExtractor.ExtractedPage(
                        null, document.getExtractedText())));
                chunks = chunkRepository.findAllByDocument_IdAndDocument_StatusNot(documentId,
                        DocumentStatus.INACTIVE);
            }
        }

        Map<String, Integer> queryVector = parseVector(chunkingService.vectorize(query));
        if (queryVector.isEmpty()) {
            return new RetrievedDocumentContext("", List.of());
        }
        List<ScoredChunk> scored = chunks.stream()
                .map(chunk -> new ScoredChunk(chunk, similarity(query, queryVector, chunk)))
                .filter(item -> item.score() > 0.01)
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .toList();

        List<DocumentSourceReference> sources = new ArrayList<>();
        StringBuilder context = new StringBuilder();
        for (ScoredChunk item : scored) {
            if (sources.size() >= MAX_CHUNKS || context.length() >= MAX_CONTEXT_CHARS) {
                break;
            }
            DocumentChunk chunk = item.chunk();
            int remaining = MAX_CONTEXT_CHARS - context.length();
            String text = chunk.getText().substring(0, Math.min(chunk.getText().length(), remaining));
            int sourceNumber = sources.size() + 1;
            sources.add(new DocumentSourceReference(chunk.getDocument().getId(),
                    chunk.getDocument().getOriginalName(), chunk.getPageNumber(), chunk.getChunkIndex()));
            context.append("[S").append(sourceNumber).append("] Documento: ")
                    .append(chunk.getDocument().getOriginalName());
            if (chunk.getPageNumber() != null) {
                context.append(", página ").append(chunk.getPageNumber());
            }
            context.append(", fragmento ").append(chunk.getChunkIndex()).append("\n")
                    .append(text).append("\n\n");
        }
        return new RetrievedDocumentContext(context.toString().strip(), sources);
    }

    private double similarity(String query, Map<String, Integer> queryVector, DocumentChunk chunk) {
        Map<String, Integer> vector = parseVector(chunk.getTermVector());
        if (vector.isEmpty()) {
            return 0;
        }
        double dot = 0;
        double queryNorm = 0;
        double chunkNorm = 0;
        for (int frequency : queryVector.values()) {
            queryNorm += frequency * frequency;
        }
        for (int frequency : vector.values()) {
            chunkNorm += frequency * frequency;
        }
        for (var term : queryVector.entrySet()) {
            dot += term.getValue() * vector.getOrDefault(term.getKey(), 0);
        }
        if (dot == 0) {
            return 0;
        }
        double cosine = dot / (Math.sqrt(queryNorm) * Math.sqrt(chunkNorm));
        String normalizedQuery = normalize(query);
        String normalizedContent = normalize(chunk.getText());
        return cosine + (normalizedContent.contains(normalizedQuery) ? 0.2 : 0);
    }

    private Map<String, Integer> parseVector(String encoded) {
        Map<String, Integer> vector = new HashMap<>();
        Matcher matcher = Pattern.compile("([\\p{L}\\p{N}]+):(\\d+)").matcher(encoded);
        while (matcher.find()) {
            vector.put(matcher.group(1), Integer.parseInt(matcher.group(2)));
        }
        return vector;
    }

    private String normalize(String text) {
        return Normalizer.normalize(text.toLowerCase(java.util.Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").strip();
    }

    private record ScoredChunk(DocumentChunk chunk, double score) {
    }
}

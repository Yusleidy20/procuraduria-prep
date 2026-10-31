package com.yusdev.procuraduriaprep.documents.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.yusdev.procuraduriaprep.documents.entity.DocumentChunk;
import com.yusdev.procuraduriaprep.documents.entity.StudyDocument;
import com.yusdev.procuraduriaprep.documents.repository.DocumentChunkRepository;

@Service
public class DocumentChunkingService {

    private static final int CHUNK_SIZE = 1800;
    private static final int OVERLAP = 200;
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}\\p{N}]+");

    private final DocumentChunkRepository chunkRepository;

    public DocumentChunkingService(DocumentChunkRepository chunkRepository) {
        this.chunkRepository = chunkRepository;
    }

    public void index(StudyDocument document, List<DocumentTextExtractor.ExtractedPage> pages) {
        chunkRepository.deleteAllByDocument_Id(document.getId());
        List<DocumentChunk> chunks = new ArrayList<>();
        int index = 0;
        for (var page : pages) {
            String text = page.text().strip();
            for (int start = 0; start < text.length();) {
                int end = Math.min(start + CHUNK_SIZE, text.length());
                if (end < text.length()) {
                    int boundary = text.lastIndexOf(' ', end);
                    if (boundary > start + CHUNK_SIZE / 2) {
                        end = boundary;
                    }
                }
                String chunk = text.substring(start, end).strip();
                if (!chunk.isBlank()) {
                    chunks.add(new DocumentChunk(document, index++, page.pageNumber(), chunk, vectorize(chunk)));
                }
                if (end >= text.length()) {
                    break;
                }
                start = Math.max(start + 1, end - OVERLAP);
            }
        }
        chunkRepository.saveAll(chunks);
    }

    public String vectorize(String text) {
        Matcher matcher = TOKEN_PATTERN.matcher(Normalizer.normalize(text.toLowerCase(java.util.Locale.ROOT),
                Normalizer.Form.NFD).replaceAll("\\p{M}+", ""));
        Map<String, Integer> frequencies = new TreeMap<>();
        while (matcher.find()) {
            String token = matcher.group();
            if (token.length() > 1 || token.chars().allMatch(Character::isDigit)) {
                frequencies.merge(token, 1, Integer::sum);
            }
        }
        return frequencies.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(java.util.stream.Collectors.joining(" "));
    }
}

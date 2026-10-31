package com.yusdev.procuraduriaprep.documents.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.yusdev.procuraduriaprep.documents.entity.DocumentChunk;
import com.yusdev.procuraduriaprep.documents.entity.StudyDocument;
import com.yusdev.procuraduriaprep.documents.repository.DocumentChunkRepository;

class DocumentChunkingServiceTest {

    @Test
    void createsOverlappingChunksWithPageTraceAndPersistedSparseTermVectors() {
        DocumentChunkRepository repository = Mockito.mock(DocumentChunkRepository.class);
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        DocumentChunkingService service = new DocumentChunkingService(repository);
        StudyDocument document = Mockito.mock(StudyDocument.class);
        when(document.getId()).thenReturn(UUID.randomUUID());
        String text = "Artículo 123 fundamento constitucional ".repeat(100);

        service.index(document, List.of(new DocumentTextExtractor.ExtractedPage(7, text)));

        ArgumentCaptor<Iterable<DocumentChunk>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(repository).saveAll(captor.capture());
        List<DocumentChunk> chunks = new ArrayList<>();
        captor.getValue().forEach(chunks::add);
        assertTrue(chunks.size() > 1);
        assertTrue(chunks.stream().allMatch(chunk -> chunk.getPageNumber() == 7));
        assertTrue(chunks.stream().allMatch(chunk -> chunk.getTermVector().contains("articulo:")));
        assertTrue(chunks.get(1).getText().contains(chunks.get(0).getText()
                .substring(chunks.get(0).getText().length() - 150)));
    }
}

package com.yusdev.procuraduriaprep.documents.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.yusdev.procuraduriaprep.documents.entity.DocumentChunk;
import com.yusdev.procuraduriaprep.documents.entity.DocumentStatus;
import com.yusdev.procuraduriaprep.documents.entity.StudyDocument;
import com.yusdev.procuraduriaprep.documents.repository.DocumentChunkRepository;
import com.yusdev.procuraduriaprep.documents.repository.StudyDocumentRepository;

class DocumentRetrievalServiceTest {

    @Test
    void ranksRelevantChunksAndReturnsDocumentPageReferences() {
        DocumentChunkRepository chunks = mock(DocumentChunkRepository.class);
        StudyDocumentRepository documents = mock(StudyDocumentRepository.class);
        DocumentChunkingService chunking = new DocumentChunkingService(chunks);
        StudyDocument document = mock(StudyDocument.class);
        UUID id = UUID.randomUUID();
        when(document.getId()).thenReturn(id);
        when(document.getOriginalName()).thenReturn("ley.pdf");
        DocumentChunk relevant = chunk(document, "articulo:2 constitucional:1 tutela:1",
                "El artículo 2 define la acción constitucional de tutela.", 3, 5);
        DocumentChunk irrelevant = chunk(document, "contrato:3 laboral:2",
                "El contrato laboral regula la relación de trabajo.", 8, 1);
        when(chunks.findAllByDocument_IdAndDocument_StatusNot(id, DocumentStatus.INACTIVE))
                .thenReturn(List.of(irrelevant, relevant));
        when(documents.findById(id)).thenReturn(java.util.Optional.of(document));
        when(document.isActive()).thenReturn(true);
        when(document.getStatus()).thenReturn(DocumentStatus.COMPLETED);
        when(document.getExtractedText()).thenReturn("texto completo");

        RetrievedDocumentContext result = new DocumentRetrievalService(chunks, documents, chunking)
                .retrieve("acción constitucional artículo 2", id);

        assertEquals(1, result.sources().size());
        assertEquals("ley.pdf", result.sources().get(0).documentName());
        assertEquals(3, result.sources().get(0).pageNumber());
        assertTrue(result.text().contains("acción constitucional de tutela"));
        verify(chunks).findAllByDocument_IdAndDocument_StatusNot(id, DocumentStatus.INACTIVE);
    }

    @Test
    void returnsEmptyContextWhenNoChunkMatches() {
        DocumentChunkRepository chunks = mock(DocumentChunkRepository.class);
        when(chunks.findAllByDocument_StatusNot(DocumentStatus.INACTIVE)).thenReturn(List.of());

        RetrievedDocumentContext result = new DocumentRetrievalService(chunks,
                mock(StudyDocumentRepository.class), new DocumentChunkingService(chunks))
                .retrieve("competencias electorales", null);

        assertTrue(result.isEmpty());
    }

    @Test
    void lazilyIndexesLegacyDocumentsBeforeGlobalSearch() {
        DocumentChunkRepository chunks = mock(DocumentChunkRepository.class);
        StudyDocumentRepository documents = mock(StudyDocumentRepository.class);
        DocumentChunkingService chunking = new DocumentChunkingService(chunks);
        StudyDocument document = mock(StudyDocument.class);
        UUID id = UUID.randomUUID();
        when(document.getId()).thenReturn(id);
        when(document.getExtractedText()).thenReturn("La Constitución reconoce la acción de tutela.");
        when(documents.findCompletedDocumentsWithoutChunks(DocumentStatus.COMPLETED))
                .thenReturn(List.of(document));
        DocumentChunk relevant = chunk(document, "constitucion:1 tutela:1 accion:1",
                "La Constitución reconoce la acción de tutela.", null, 0);
        when(chunks.findAllByDocument_StatusNot(DocumentStatus.INACTIVE)).thenReturn(List.of(relevant));

        RetrievedDocumentContext result = new DocumentRetrievalService(chunks, documents, chunking)
                .retrieve("acción de tutela", null);

        assertEquals(1, result.sources().size());
        assertNull(result.sources().get(0).pageNumber());
        verify(chunks).deleteAllByDocument_Id(id);
        verify(chunks).saveAll(anyList());
    }

    private DocumentChunk chunk(StudyDocument document, String vector, String text, Integer page, int index) {
        DocumentChunk chunk = mock(DocumentChunk.class);
        when(chunk.getDocument()).thenReturn(document);
        when(chunk.getTermVector()).thenReturn(vector);
        when(chunk.getText()).thenReturn(text);
        when(chunk.getPageNumber()).thenReturn(page);
        when(chunk.getChunkIndex()).thenReturn(index);
        return chunk;
    }
}

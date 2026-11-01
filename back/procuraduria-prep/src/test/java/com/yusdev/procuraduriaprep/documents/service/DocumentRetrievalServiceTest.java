package com.yusdev.procuraduriaprep.documents.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertTrue(result.libraryEmpty());
    }

    @Test
    void searchesEveryActiveDocumentWhenNoDocumentFilterIsProvided() {
        DocumentChunkRepository chunks = mock(DocumentChunkRepository.class);
        StudyDocumentRepository documents = mock(StudyDocumentRepository.class);
        DocumentChunkingService chunking = new DocumentChunkingService(chunks);
        StudyDocument document = mock(StudyDocument.class);
        UUID id = UUID.randomUUID();
        when(document.getId()).thenReturn(id);
        when(document.getOriginalName()).thenReturn("gestion-publica.pdf");
        when(documents.countByStatusNot(DocumentStatus.INACTIVE)).thenReturn(1L);
        DocumentChunk relevant = chunk(document,
                "gestion:2 publica:1 funcionamiento:1 estado:1",
                "La gestión pública define el funcionamiento del Estado.", 2, 4);
        when(chunks.findAllByDocument_StatusNot(DocumentStatus.INACTIVE)).thenReturn(List.of(relevant));

        RetrievedDocumentContext result = new DocumentRetrievalService(chunks, documents, chunking)
                .retrieve("Gestión pública y funcionamiento del Estado", null);

        assertEquals(1, result.sources().size());
        assertEquals("gestion-publica.pdf", result.sources().get(0).documentName());
        assertEquals(2, result.sources().get(0).pageNumber());
        assertTrue(result.text().contains("funcionamiento del Estado"));
        assertFalse(result.libraryEmpty());
        verify(chunks).findAllByDocument_StatusNot(DocumentStatus.INACTIVE);
    }

    @Test
    void skipsLegacyDocumentsWithBlankExtractedTextWithoutPassingEmptyTextToIndexing() {
        DocumentChunkRepository chunks = mock(DocumentChunkRepository.class);
        StudyDocumentRepository documents = mock(StudyDocumentRepository.class);
        DocumentChunkingService chunking = new DocumentChunkingService(chunks);
        StudyDocument blankDocument = mock(StudyDocument.class);
        when(blankDocument.getExtractedText()).thenReturn("   ");
        when(documents.findCompletedDocumentsWithoutChunks(DocumentStatus.COMPLETED))
                .thenReturn(List.of(blankDocument));
        when(chunks.findAllByDocument_StatusNot(DocumentStatus.INACTIVE)).thenReturn(List.of());
        when(documents.countByStatusNot(DocumentStatus.INACTIVE)).thenReturn(1L);

        RetrievedDocumentContext result = new DocumentRetrievalService(chunks, documents, chunking)
                .retrieve("Gestión pública", null);

        assertTrue(result.isEmpty());
        assertFalse(result.libraryEmpty());
        verify(chunks).findAllByDocument_StatusNot(DocumentStatus.INACTIVE);
        verify(chunks, org.mockito.Mockito.never()).deleteAllByDocument_Id(
                org.mockito.ArgumentMatchers.any());
        verify(chunks, org.mockito.Mockito.never()).saveAll(anyList());
    }

    @Test
    void usesPreviousStudentQuestionsToRetrieveContextForFollowUpQuestions() {
        DocumentChunkRepository chunks = mock(DocumentChunkRepository.class);
        StudyDocumentRepository documents = mock(StudyDocumentRepository.class);
        DocumentChunkingService chunking = new DocumentChunkingService(chunks);
        StudyDocument document = mock(StudyDocument.class);
        UUID id = UUID.randomUUID();
        when(document.getId()).thenReturn(id);
        when(document.getOriginalName()).thenReturn("constitucion.pdf");
        when(document.isActive()).thenReturn(true);
        when(document.getStatus()).thenReturn(DocumentStatus.COMPLETED);
        when(document.getExtractedText()).thenReturn("texto completo");
        DocumentChunk relevant = chunk(document, "accion:1 constitucional:1 tutela:1",
                "La Constitución reconoce la acción de tutela.", null, 2);
        when(chunks.findAllByDocument_StatusNot(DocumentStatus.INACTIVE)).thenReturn(List.of(relevant));

        RetrievedDocumentContext result = new DocumentRetrievalService(chunks, documents, chunking)
                .retrieve("¿Puedes profundizar en eso?", null, List.of("Explícame la acción constitucional de tutela"));

        assertEquals(1, result.sources().size());
        assertEquals("constitucion.pdf", result.sources().get(0).documentName());
        assertTrue(result.text().contains("acción de tutela"));
    }

    @Test
    void retriesRetrievalWithSpecificQuestionTermsWhenInitialSearchHasNoMatch() {
        DocumentChunkRepository chunks = mock(DocumentChunkRepository.class);
        StudyDocumentRepository documents = mock(StudyDocumentRepository.class);
        DocumentChunkingService chunking = new DocumentChunkingService(chunks);
        StudyDocument document = mock(StudyDocument.class);
        when(document.getId()).thenReturn(UUID.randomUUID());
        when(document.getOriginalName()).thenReturn("manual.pdf");
        String lowSignalVector = java.util.stream.IntStream.range(0, 9000)
                .mapToObj(index -> "ruido" + index + ":1")
                .collect(java.util.stream.Collectors.joining(" ")) + " concepto:1";
        DocumentChunk relevant = chunk(document, lowSignalVector, "concepto jurídico", 6, 1);
        when(chunks.findAllByDocument_StatusNot(DocumentStatus.INACTIVE)).thenReturn(List.of(relevant));

        RetrievedDocumentContext result = new DocumentRetrievalService(chunks, documents, chunking)
                .retrieve("¿Qué es el concepto?", null);

        assertEquals(1, result.sources().size());
        assertEquals(6, result.sources().get(0).pageNumber());
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

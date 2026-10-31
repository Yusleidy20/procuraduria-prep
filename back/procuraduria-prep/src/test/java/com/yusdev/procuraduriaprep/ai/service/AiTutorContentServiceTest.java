package com.yusdev.procuraduriaprep.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.yusdev.procuraduriaprep.ai.dto.DocumentSourceReference;
import com.yusdev.procuraduriaprep.ai.dto.ExplainTopicRequest;
import com.yusdev.procuraduriaprep.ai.dto.GenerateStudyContentRequest;
import com.yusdev.procuraduriaprep.documents.service.DocumentRetrievalService;
import com.yusdev.procuraduriaprep.documents.service.RetrievedDocumentContext;
import com.yusdev.procuraduriaprep.study.entity.DifficultyLevel;

import jakarta.validation.Validation;
import tools.jackson.databind.json.JsonMapper;

class AiTutorContentServiceTest {

    @Test
    void preservesExplanationFlashcardAndQuestionContractsForAnyProvider() {
        var objectMapper = JsonMapper.builder().build();
        var validatorFactory = Validation.buildDefaultValidatorFactory();
        try {
            var retrieval = Mockito.mock(DocumentRetrievalService.class);
            Mockito.when(retrieval.retrieve("Tema", null)).thenReturn(
                    new RetrievedDocumentContext("Material fuente", List.of(
                            new DocumentSourceReference(UUID.randomUUID(), "fuente.pdf", 1, 0))));
            var service = new AiTutorContentService(objectMapper,
                    new AiOutputValidator(validatorFactory.getValidator()),
                    retrieval);
            AiProvider provider = (prompt, schema) -> {
                if (prompt.startsWith("Actúa como tutor")) {
                    return """
                            {"explanation":"Concepto explicado","keyPoints":"Punto clave"}
                            """;
                }
                if (prompt.contains("tarjetas de estudio")) {
                    return """
                            {"flashcards":[{"question":"¿Qué es?","answer":"Concepto","explanation":"Detalle","difficulty":"BASIC"}]}
                            """;
                }
                return """
                        {"questions":[{"statement":"Pregunta","options":["A","B"],"correctOptionIndex":1,"explanation":"Razón"}]}
                        """;
            };
            var studyRequest = new GenerateStudyContentRequest(UUID.randomUUID(), "Tema", null, null,
                    DifficultyLevel.BASIC, 1);

            var explanation = service.explainTopic(new ExplainTopicRequest("Tema", null, null, null), provider);
            var flashcards = service.generateFlashcards(studyRequest, provider);
            var questions = service.generateQuestions(studyRequest, provider);

            assertEquals("Concepto explicado", explanation.explanation());
            assertEquals(1, flashcards.size());
            assertEquals("¿Qué es?", flashcards.get(0).question());
            assertEquals(1, questions.size());
            assertEquals(1, questions.get(0).correctOptionIndex());
            assertEquals("fuente.pdf", explanation.sources().get(0).documentName());
            assertEquals(1, questions.get(0).sources().get(0).pageNumber());
        } finally {
            validatorFactory.close();
        }
    }

    @Test
    void doesNotCallProviderWhenNoRelevantDocumentContextExists() {
        var validatorFactory = Validation.buildDefaultValidatorFactory();
        try {
            var retrieval = Mockito.mock(DocumentRetrievalService.class);
            Mockito.when(retrieval.retrieve("Tema", null))
                    .thenReturn(new RetrievedDocumentContext("", java.util.List.of()));
            AiProvider provider = Mockito.mock(AiProvider.class);
            var service = new AiTutorContentService(JsonMapper.builder().build(),
                    new AiOutputValidator(validatorFactory.getValidator()), retrieval);

            var result = service.explainTopic(new ExplainTopicRequest("Tema", null, null, null), provider);

            assertEquals(List.of(), result.sources());
            assertTrue(result.explanation().contains("No encontré información suficiente"));
            Mockito.verifyNoInteractions(provider);
        } finally {
            validatorFactory.close();
        }
    }

    @Test
    void refusesToGenerateFlashcardsWithoutRelevantDocumentContext() {
        var validatorFactory = Validation.buildDefaultValidatorFactory();
        try {
            var retrieval = Mockito.mock(DocumentRetrievalService.class);
            Mockito.when(retrieval.retrieve("Tema", null))
                    .thenReturn(new RetrievedDocumentContext("", java.util.List.of()));
            AiProvider provider = Mockito.mock(AiProvider.class);
            var service = new AiTutorContentService(JsonMapper.builder().build(),
                    new AiOutputValidator(validatorFactory.getValidator()), retrieval);
            var request = new GenerateStudyContentRequest(UUID.randomUUID(), "Tema", null, null,
                    DifficultyLevel.BASIC, 1);

            assertThrows(AiInsufficientContextException.class, () -> service.generateFlashcards(request, provider));
            Mockito.verifyNoInteractions(provider);
        } finally {
            validatorFactory.close();
        }
    }
}

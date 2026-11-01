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
import com.yusdev.procuraduriaprep.ai.dto.StudyConversationContextMessage;
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
            assertTrue(result.explanation().contains("No encontré fragmentos relevantes"));
            Mockito.verifyNoInteractions(provider);
        } finally {
            validatorFactory.close();
        }
    }

    @Test
    void searchesEntireLibraryForNaturalLanguageTopicAndPassesRetrievedEvidenceToProvider() {
        var validatorFactory = Validation.buildDefaultValidatorFactory();
        try {
            var retrieval = Mockito.mock(DocumentRetrievalService.class);
            var source = new DocumentSourceReference(UUID.randomUUID(), "manual.pdf", 9, 2);
            String topic = "Gestión pública y funcionamiento del Estado";
            Mockito.when(retrieval.retrieve(topic, null))
                    .thenReturn(new RetrievedDocumentContext(
                            "[S1] Documento: manual.pdf, página 9, fragmento 2\nLa función pública organiza la gestión estatal.",
                            List.of(source)));
            var service = new AiTutorContentService(JsonMapper.builder().build(),
                    new AiOutputValidator(validatorFactory.getValidator()), retrieval);
            var prompts = new java.util.ArrayList<String>();
            AiProvider provider = (prompt, schema) -> {
                prompts.add(prompt);
                return """
                        {"explanation":"La fuente describe la gestión estatal [S1].","keyPoints":"Función pública"}
                        """;
            };

            var response = service.explainTopic(
                    new ExplainTopicRequest(topic, null, "Inicial", null), provider);

            Mockito.verify(retrieval).retrieve(topic, null);
            assertTrue(prompts.get(0).contains(topic));
            assertTrue(prompts.get(0).contains("La función pública organiza la gestión estatal."));
            assertEquals(List.of(source), response.sources());
        } finally {
            validatorFactory.close();
        }
    }

    @Test
    void distinguishesAnEmptyLibraryFromDocumentsWithoutRelevantFragments() {
        var validatorFactory = Validation.buildDefaultValidatorFactory();
        try {
            var retrieval = Mockito.mock(DocumentRetrievalService.class);
            Mockito.when(retrieval.retrieve("Gestión pública", null))
                    .thenReturn(new RetrievedDocumentContext("", List.of(), true));
            var service = new AiTutorContentService(JsonMapper.builder().build(),
                    new AiOutputValidator(validatorFactory.getValidator()), retrieval);

            var response = service.explainTopic(
                    new ExplainTopicRequest("Gestión pública", null, "Inicial", null),
                    Mockito.mock(AiProvider.class));

            assertTrue(response.explanation().contains("biblioteca aún no tiene documentos"));
            assertTrue(response.keyPoints().contains("Carga material"));
        } finally {
            validatorFactory.close();
        }
    }

    @Test
    void searchesUsingTheFollowUpAndPreviousQuestionsAndReturnsOnlyRetrievedSources() {
        var validatorFactory = Validation.buildDefaultValidatorFactory();
        try {
            var retrieval = Mockito.mock(DocumentRetrievalService.class);
            var source = new DocumentSourceReference(UUID.randomUUID(), "ley.pdf", 4, 2);
            Mockito.when(retrieval.retrieve(
                    "¿Qué diferencia hay?", null, List.of("Explícame la falta disciplinaria")))
                    .thenReturn(new RetrievedDocumentContext("Fragmento [S1]: evidencia documental", List.of(source)));
            var service = new AiTutorContentService(JsonMapper.builder().build(),
                    new AiOutputValidator(validatorFactory.getValidator()), retrieval);
            var request = new ExplainTopicRequest("¿Qué diferencia hay?", null, "Intermedio", null,
                    List.of("Explícame la falta disciplinaria"), List.of(
                            new StudyConversationContextMessage("USER", "Explícame la falta disciplinaria"),
                            new StudyConversationContextMessage("ASSISTANT", "La norma describe sus elementos.")));
            var prompts = new java.util.ArrayList<String>();
            AiProvider provider = (prompt, schema) -> {
                prompts.add(prompt);
                return """
                        {"explanation":"Diferencia basada en la fuente [S1].","keyPoints":"Revisar el contexto."}
                        """;
            };

            var response = service.explainTopic(request, provider);

            Mockito.verify(retrieval).retrieve(
                    request.topicName(), null, request.previousQuestions());
            assertEquals(List.of(source), response.sources());
            assertTrue(prompts.get(0).contains("Analiza la intención"));
            assertTrue(prompts.get(0).contains("No afirmes vigencia normativa"));
            assertTrue(prompts.get(0).contains("Explícame la falta disciplinaria"));
            assertTrue(prompts.get(0).contains("Historial reciente de conversación (solo contexto, no evidencia jurídica)"));
            assertTrue(prompts.get(0).contains("ASSISTANT: La norma describe sus elementos."));
            assertTrue(prompts.get(0).contains("Fragmento [S1]"));
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

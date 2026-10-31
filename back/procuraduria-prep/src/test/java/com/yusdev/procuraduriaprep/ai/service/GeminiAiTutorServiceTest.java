package com.yusdev.procuraduriaprep.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import java.io.IOException;
import javax.net.ssl.SSLHandshakeException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.yusdev.procuraduriaprep.ai.dto.GenerateStudyContentRequest;
import com.yusdev.procuraduriaprep.documents.service.DocumentRetrievalService;
import com.yusdev.procuraduriaprep.documents.service.RetrievedDocumentContext;
import com.yusdev.procuraduriaprep.shared.exception.InvalidRequestException;
import com.yusdev.procuraduriaprep.study.entity.DifficultyLevel;

import jakarta.validation.Validation;
import tools.jackson.databind.json.JsonMapper;

class GeminiAiTutorServiceTest {

    private final AiOutputValidator outputValidator = new AiOutputValidator(
            Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void reportsUnavailableWhenApiKeyIsNotConfigured() {
        GeminiAiTutorService service = service(RestClient.builder(), "");

        assertThrows(AiProviderUnavailableException.class,
                () -> service.generateQuestions(request()));
    }

    @Test
    void sendsStructuredRequestAndValidatesGeminiQuestionOutput() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andExpect(content().string(containsString("Material fuente")))
                .andRespond(withSuccess("""
                        {
                          "candidates": [{
                            "content": {
                              "parts": [{
                                "text": "{\\"questions\\":[{\\"statement\\":\\"Pregunta\\",\\"options\\":[\\"A\\",\\"B\\"],\\"correctOptionIndex\\":1,\\"explanation\\":\\"Razón\\"}]}"
                              }]
                            }
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        var result = service(builder, "test-key").generateQuestions(request());
        server.verify();

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).correctOptionIndex());
        assertEquals("Razón", result.get(0).explanation());
    }

    @Test
    void rejectsMalformedGeneratedQuizContent() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"))
                .andRespond(withSuccess("""
                        {
                          "candidates": [{
                            "content": {
                              "parts": [{
                                "text": "{\\"questions\\":[{\\"statement\\":\\"Pregunta\\",\\"options\\":[\\"A\\",\\"B\\"],\\"correctOptionIndex\\":3,\\"explanation\\":\\"Razón\\"}]}"
                              }]
                            }
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThrows(AiOutputValidationException.class,
                () -> service(builder, "test-key").generateQuestions(request()));
        server.verify();
    }

    @Test
    void logsProviderHttpStatusAndSanitizedErrorWhileKeepingPublicErrorGeneric() {
        String providerMessage = "models/gemini-2.5-flash is not found for API version v1beta, "
                + "or is not supported for generateContent. Call ListModels.";
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":{"code":404,"message":"%s received test-key","status":"NOT_FOUND"}}
                                """.formatted(providerMessage)));

        Logger logger = (Logger) LoggerFactory.getLogger(GeminiAiTutorService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        AiProviderException exception;
        try {
            exception = assertThrows(AiProviderException.class,
                    () -> service(builder, "test-key").generateQuestions(request()));
        } finally {
            logger.detachAppender(appender);
        }

        server.verify();
        assertEquals("No fue posible obtener una respuesta válida del proveedor de IA", exception.getMessage());
        assertEquals(1, appender.list.size());
        String diagnostic = appender.list.get(0).getFormattedMessage();
        assertTrue(diagnostic.contains("HTTP 404"));
        assertTrue(diagnostic.contains(providerMessage));
        assertFalse(diagnostic.contains("test-key"));
    }

    @Test
    void logsNestedNetworkCauseAndStackTraceWithoutLoggingRequestData() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withException(new IOException("transport failure",
                                new SSLHandshakeException("certificate chain rejected"))));

        Logger logger = (Logger) LoggerFactory.getLogger(GeminiAiTutorService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        AiProviderException exception;
        try {
            exception = assertThrows(AiProviderException.class,
                    () -> service(builder, "test-key").generateQuestions(request()));
        } finally {
            logger.detachAppender(appender);
        }

        server.verify();
        assertTrue(exception.isRetryable());
        assertEquals(null, exception.getHttpStatus());
        assertEquals(1, appender.list.size());
        ILoggingEvent event = appender.list.get(0);
        assertTrue(event.getFormattedMessage().contains("model=gemini-2.5-flash"));
        assertTrue(event.getFormattedMessage().contains("rootCauseType=SSLHandshakeException"));
        assertTrue(event.getFormattedMessage().contains("certificate chain rejected"));
        assertFalse(event.getFormattedMessage().contains("test-key"));
        assertFalse(event.getFormattedMessage().contains("Material fuente"));
        assertTrue(event.getThrowableProxy().getClassName().contains("ResourceAccessException"));
        assertTrue(event.getThrowableProxy().getCause().getClassName().contains("IOException"));
        assertTrue(event.getThrowableProxy().getCause().getCause().getClassName()
                .contains("SSLHandshakeException"));
    }

    @Test
    void sendsProcessedDocumentTextAsGeminiSource() {
        UUID documentId = UUID.randomUUID();
        DocumentRetrievalService retrieval = mock(DocumentRetrievalService.class);
        when(retrieval.retrieve("Acción de tutela", documentId)).thenReturn(
                new RetrievedDocumentContext("Contenido extraído del documento", java.util.List.of()));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"))
                .andExpect(content().string(containsString("Contenido extraído del documento")))
                .andRespond(withSuccess("""
                        {
                          "candidates": [{
                            "content": {
                              "parts": [{
                                "text": "{\\"questions\\":[{\\"statement\\":\\"Pregunta\\",\\"options\\":[\\"A\\",\\"B\\"],\\"correctOptionIndex\\":1,\\"explanation\\":\\"Razón\\"}]}"
                              }]
                            }
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        service(builder, "test-key", retrieval).generateQuestions(request(documentId));

        server.verify();
    }

    @Test
    void rejectsDocumentWithoutProcessedTextBeforeCallingGemini() {
        UUID documentId = UUID.randomUUID();
        DocumentRetrievalService retrieval = mock(DocumentRetrievalService.class);
        when(retrieval.retrieve("Acción de tutela", documentId))
                .thenThrow(new InvalidRequestException("El documento aún no tiene texto procesado"));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        assertThrows(InvalidRequestException.class,
                () -> service(builder, "test-key", retrieval).generateQuestions(request(documentId)));
        server.verify();
    }

    private GeminiAiTutorService service(RestClient.Builder builder, String apiKey) {
        return service(builder, apiKey, mock(DocumentRetrievalService.class));
    }

    private GeminiAiTutorService service(RestClient.Builder builder, String apiKey,
                                         DocumentRetrievalService retrieval) {
        AiTutorContentService contentService = new AiTutorContentService(
                JsonMapper.builder().build(), outputValidator, retrieval);
        return new GeminiAiTutorService(builder, JsonMapper.builder().build(), contentService, apiKey,
                "gemini-2.5-flash");
    }

    private GenerateStudyContentRequest request() {
        return request(null);
    }

    private GenerateStudyContentRequest request(UUID sourceDocumentId) {
        return new GenerateStudyContentRequest(java.util.UUID.randomUUID(), "Acción de tutela",
                sourceDocumentId == null ? "Material fuente" : null, sourceDocumentId, DifficultyLevel.BASIC, 1);
    }
}

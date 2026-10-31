package com.yusdev.procuraduriaprep.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

import java.util.Map;
import java.util.UUID;
import java.io.IOException;
import java.net.http.HttpTimeoutException;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.yusdev.procuraduriaprep.ai.dto.GenerateStudyContentRequest;
import com.yusdev.procuraduriaprep.documents.repository.StudyDocumentRepository;
import com.yusdev.procuraduriaprep.study.entity.DifficultyLevel;

import jakarta.validation.Validation;
import tools.jackson.databind.json.JsonMapper;

class GroqAiTutorServiceTest {

    @Test
    void mapsChatCompletionIntoExistingQuestionContract() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(content().string(Matchers.allOf(
                        Matchers.containsString("test-llama-model"),
                        Matchers.containsString("json_object"),
                        Matchers.containsString("Material fuente"))))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess("""
                        {
                          "choices": [{
                            "message": {
                              "role": "assistant",
                              "content": "{\\"questions\\":[{\\"statement\\":\\"Pregunta\\",\\"options\\":[\\"A\\",\\"B\\"],\\"correctOptionIndex\\":1,\\"explanation\\":\\"Razón\\"}]}"
                            }
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        var objectMapper = JsonMapper.builder().build();
        var validator = new AiOutputValidator(Validation.buildDefaultValidatorFactory().getValidator());
        var contentService = new AiTutorContentService(objectMapper, validator,
                org.mockito.Mockito.mock(com.yusdev.procuraduriaprep.documents.service.DocumentRetrievalService.class));
        var groq = new GroqAiTutorService(builder, "test-key", "test-llama-model",
                "https://api.groq.com/openai/v1/");
        var request = new GenerateStudyContentRequest(UUID.randomUUID(), "Tema", "Material fuente", null,
                DifficultyLevel.BASIC, 1);

        var result = contentService.generateQuestions(request, groq);

        assertEquals(1, result.size());
        assertEquals("Pregunta", result.get(0).statement());
        assertEquals(1, result.get(0).correctOptionIndex());
        assertEquals("Razón", result.get(0).explanation());
        server.verify();
    }

    @Test
    void classifiesBadRequestAsNonRetryableWithoutLeakingProviderMessage() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"invalid request\"}}"));
        var groq = new GroqAiTutorService(builder, "test-key", "test-model", "https://api.groq.com/openai/v1");

        AiProviderException exception = assertThrows(AiProviderException.class,
                () -> groq.generate("prompt", Map.of()));

        assertFalse(exception.isRetryable());
        assertEquals(400, exception.getHttpStatus());
        assertEquals("No fue posible obtener una respuesta del proveedor Groq", exception.getMessage());
        server.verify();
    }

    @Test
    void requiresAnExplicitModelAndApiKey() {
        var groq = new GroqAiTutorService(RestClient.builder(), "", "", "https://api.groq.com/openai/v1");

        assertThrows(AiProviderUnavailableException.class, () -> groq.generate("prompt", Map.of()));
    }

    @Test
    void returnsSafeSpecificErrorsForRateLimitAndInvalidApiKey() {
        assertProviderError(HttpStatus.TOO_MANY_REQUESTS,
                "Se alcanzó el límite de solicitudes del proveedor Groq", 429);
        assertProviderError(HttpStatus.UNAUTHORIZED,
                "La configuración de la API key de Groq no es válida", 401);
        assertProviderError(HttpStatus.SERVICE_UNAVAILABLE,
                "No fue posible obtener una respuesta del proveedor Groq", 503);
    }

    @Test
    void mapsTimeoutToControlledPublicError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withException(new IOException("transport", new HttpTimeoutException("timed out"))));
        var groq = new GroqAiTutorService(builder, "test-key", "test-model", "https://api.groq.com/openai/v1");

        AiProviderException exception = assertThrows(AiProviderException.class,
                () -> groq.generate("prompt", Map.of()));

        assertEquals("El proveedor Groq agotó el tiempo de espera", exception.getMessage());
        assertFalse(exception.getMessage().contains("timed out"));
        server.verify();
    }

    private void assertProviderError(HttpStatus status, String expectedMessage, int expectedStatus) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withStatus(status)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"private provider details\"}}"));
        var groq = new GroqAiTutorService(builder, "test-key", "test-model", "https://api.groq.com/openai/v1");

        AiProviderException exception = assertThrows(AiProviderException.class,
                () -> groq.generate("prompt", Map.of()));

        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(expectedStatus, exception.getHttpStatus());
        assertFalse(exception.getMessage().contains("private provider details"));
        server.verify();
    }
}

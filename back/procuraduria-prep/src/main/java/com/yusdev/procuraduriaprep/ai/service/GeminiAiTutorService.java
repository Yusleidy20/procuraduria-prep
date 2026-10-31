package com.yusdev.procuraduriaprep.ai.service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.yusdev.procuraduriaprep.ai.dto.ExplainTopicRequest;
import com.yusdev.procuraduriaprep.ai.dto.GenerateStudyContentRequest;
import com.yusdev.procuraduriaprep.ai.dto.GeneratedFlashcard;
import com.yusdev.procuraduriaprep.ai.dto.GeneratedQuizQuestion;
import com.yusdev.procuraduriaprep.ai.dto.TopicExplanationResponse;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class GeminiAiTutorService implements AiTutorService, AiProvider {

    private static final Logger logger = LoggerFactory.getLogger(GeminiAiTutorService.class);
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent";
    private static final Pattern SENSITIVE_VALUE = Pattern.compile(
            "(?i)AIza[0-9A-Za-z_-]{20,}|Bearer\\s+[^\\s,;]+|"
                    + "(?:api[-_ ]?key|authorization|password|token)\\s*[:=]\\s*[^\\s,;]+|"
                    + "[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}");
    private static final int MAX_PROVIDER_MESSAGE_LENGTH = 500;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AiTutorContentService contentService;
    private final String apiKey;
    private final String model;

    public GeminiAiTutorService(@Qualifier("geminiRestClientBuilder") RestClient.Builder restClientBuilder,
                                ObjectMapper objectMapper,
                                AiTutorContentService contentService,
                                @Value("${app.ai.gemini.api-key:}") String apiKey,
                                @Value("${app.ai.gemini.model:gemini-3.8-flash}") String model) {
        this.restClient = restClientBuilder.clone().build();
        this.objectMapper = objectMapper;
        this.contentService = contentService;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
    }

    @Override
    public TopicExplanationResponse explainTopic(ExplainTopicRequest request) {
        return contentService.explainTopic(request, this);
    }

    @Override
    public List<GeneratedFlashcard> generateFlashcards(GenerateStudyContentRequest request) {
        return contentService.generateFlashcards(request, this);
    }

    @Override
    public List<GeneratedQuizQuestion> generateQuestions(GenerateStudyContentRequest request) {
        return contentService.generateQuestions(request, this);
    }

    @Override
    public String generate(String prompt, Map<String, Object> responseSchema) {
        if (apiKey.isBlank()) {
            throw new AiProviderUnavailableException(
                    "Gemini no está configurado; define GEMINI_API_KEY para habilitar las funciones de IA");
        }
        Map<String, Object> request = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", responseSchema));
        try {
            GeminiResponse response = restClient.post()
                    .uri(API_URL, model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);
            if (response == null || response.candidates() == null || response.candidates().isEmpty()
                    || response.candidates().get(0).content() == null
                    || response.candidates().get(0).content().parts() == null
                    || response.candidates().get(0).content().parts().isEmpty()) {
                throw new AiProviderException("Gemini devolvió una respuesta vacía");
            }
            String text = response.candidates().get(0).content().parts().get(0).text();
            if (text == null || text.isBlank()) {
                throw new AiProviderException("Gemini no devolvió contenido de texto");
            }
            return text;
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            String providerMessage = extractProviderMessage(exception.getResponseBodyAsString());
            logger.warn("Gemini request failed: HTTP {} - {}", status,
                    sanitizeProviderMessage(providerMessage, prompt));
            throw new AiProviderException("No fue posible obtener una respuesta válida del proveedor de IA",
                    "Gemini", isRetryableStatus(status), status);
        } catch (RestClientException exception) {
            Throwable rootCause = getRootCause(exception);
            logger.warn("Gemini temporary transport diagnostics: no HTTP response; model={}, rootCauseType={}, "
                            + "rootCauseMessage={}", model, rootCause.getClass().getSimpleName(),
                    sanitizeProviderMessage(rootCause.getMessage() == null ? "No detail" : rootCause.getMessage(),
                            prompt), exception);
            throw new AiProviderException("No fue posible obtener una respuesta válida del proveedor de IA",
                    "Gemini", true, null);
        }
    }

    private boolean isRetryableStatus(int status) {
        return status == 408 || status == 429 || status == 500 || status == 502 || status == 503 || status == 504;
    }

    private Throwable getRootCause(Throwable exception) {
        Throwable rootCause = exception;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    private String extractProviderMessage(String responseBody) {
        try {
            GeminiErrorEnvelope error = objectMapper.readValue(responseBody, GeminiErrorEnvelope.class);
            return error == null || error.error() == null || error.error().message() == null
                    ? "No provider message returned" : error.error().message();
        } catch (JacksonException exception) {
            return "Provider returned an unreadable error response";
        }
    }

    private String sanitizeProviderMessage(String message, String prompt) {
        String sanitized = message.replace(apiKey, "[REDACTED]");
        if (!prompt.isBlank()) {
            sanitized = sanitized.replace(prompt, "[REDACTED]");
        }
        sanitized = SENSITIVE_VALUE.matcher(sanitized).replaceAll("[REDACTED]")
                .replaceAll("[\\p{Cntrl}&&[^\\t]]", " ")
                .replaceAll("[\\t\\r\\n]+", " ")
                .trim();
        return sanitized.length() > MAX_PROVIDER_MESSAGE_LENGTH
                ? sanitized.substring(0, MAX_PROVIDER_MESSAGE_LENGTH) : sanitized;
    }

    private record GeminiResponse(List<Candidate> candidates) {
    }

    private record Candidate(Content content) {
    }

    private record Content(List<Part> parts) {
    }

    private record Part(String text) {
    }

    private record GeminiErrorEnvelope(GeminiError error) {
    }

    private record GeminiError(String message) {
    }
}

package com.yusdev.procuraduriaprep.ai.service;

import java.util.List;
import java.util.Map;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class GroqAiTutorService implements AiProvider {

    private static final Logger logger = LoggerFactory.getLogger(GroqAiTutorService.class);
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final String endpoint;
    private final int maxCompletionTokens;

    @Autowired
    public GroqAiTutorService(@Qualifier("groqRestClientBuilder") RestClient.Builder restClientBuilder,
                              @Value("${app.ai.groq.api-key:}") String apiKey,
                              @Value("${app.ai.groq.model:}") String model,
                              @Value("${app.ai.groq.base-url:https://api.groq.com/openai/v1}") String baseUrl,
                              @Value("${app.ai.groq.max-completion-tokens:3000}") int maxCompletionTokens) {
        this.restClient = restClientBuilder.clone().build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null ? "" : model.trim();
        this.endpoint = trimTrailingSlashes(baseUrl) + CHAT_COMPLETIONS_PATH;
        this.maxCompletionTokens = maxCompletionTokens;
    }

    public GroqAiTutorService(RestClient.Builder restClientBuilder, String apiKey, String model, String baseUrl) {
        this(restClientBuilder, apiKey, model, baseUrl, 3000);
    }

    @Override
    public String generate(String prompt, Map<String, Object> responseSchema) {
        if (apiKey.isBlank()) {
            throw new AiProviderUnavailableException(
                    "Groq no está configurado; define GROQ_API_KEY para habilitar el proveedor alternativo");
        }
        if (model.isBlank()) {
            throw new AiProviderUnavailableException(
                    "Groq no está configurado; define GROQ_MODEL con un modelo activo de Groq");
        }

        Map<String, Object> request = Map.of(
                "model", model,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", prompt)),
                "response_format", Map.of("type", "json_object"),
                "max_completion_tokens", maxCompletionTokens);
        long startedAt = System.nanoTime();
        try {
            GroqResponse response = restClient.post()
                    .uri(endpoint)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GroqResponse.class);
            if (response == null || response.choices() == null || response.choices().isEmpty()
                    || response.choices().get(0).message() == null) {
                throw new AiProviderException("Groq devolvió una respuesta vacía");
            }
            String text = response.choices().get(0).message().content();
            if (text == null || text.isBlank()) {
                throw new AiProviderException("Groq no devolvió contenido de texto");
            }
            logger.info("Groq generation completed; latencyMs={}", elapsedMillis(startedAt));
            return text;
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            logger.warn("Groq request failed: HTTP {}, latencyMs={}", status, elapsedMillis(startedAt));
            throw new AiProviderException(publicMessageForStatus(status), "Groq",
                    isRetryableStatus(status), status, status == 408 || status == 504);
        } catch (RestClientException exception) {
            Throwable rootCause = getRootCause(exception);
            logger.warn("Groq request failed without an HTTP response; model={}, rootCauseType={}, latencyMs={}",
                    model, rootCause.getClass().getSimpleName(), elapsedMillis(startedAt));
            String publicMessage = rootCause instanceof HttpTimeoutException
                    || rootCause instanceof SocketTimeoutException
                    ? "El proveedor Groq agotó el tiempo de espera"
                    : "No fue posible obtener una respuesta del proveedor Groq";
            throw new AiProviderException(publicMessage,
                    "Groq", true, null,
                    rootCause instanceof HttpTimeoutException || rootCause instanceof SocketTimeoutException);
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private boolean isRetryableStatus(int status) {
        return status == 408 || status == 429 || status == 500 || status == 502 || status == 503 || status == 504;
    }

    private String publicMessageForStatus(int status) {
        if (status == 401 || status == 403) {
            return "La configuración de la API key de Groq no es válida";
        }
        if (status == 429) {
            return "Se alcanzó el límite de solicitudes del proveedor Groq";
        }
        return "No fue posible obtener una respuesta del proveedor Groq";
    }

    private String trimTrailingSlashes(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("app.ai.groq.base-url no puede estar vacío");
        }
        return baseUrl.replaceAll("/+$", "");
    }

    private Throwable getRootCause(Throwable exception) {
        Throwable rootCause = exception;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    private record GroqResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }

    private record Message(String content) {
    }
}

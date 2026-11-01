package com.yusdev.procuraduriaprep.ai.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.yusdev.procuraduriaprep.ai.dto.ExplainTopicRequest;
import com.yusdev.procuraduriaprep.ai.dto.GenerateStudyContentRequest;
import com.yusdev.procuraduriaprep.ai.dto.GeneratedFlashcard;
import com.yusdev.procuraduriaprep.ai.dto.GeneratedQuizQuestion;
import com.yusdev.procuraduriaprep.ai.dto.TopicExplanationResponse;

@Service
@Primary
public class AiProviderCoordinator implements AiTutorService {

    private static final Logger logger = LoggerFactory.getLogger(AiProviderCoordinator.class);

    private final AiTutorContentService contentService;
    private final AiProvider gemini;
    private final AiProvider groq;
    private final String primaryProvider;
    private final boolean fallbackEnabled;

    public AiProviderCoordinator(AiTutorContentService contentService, GeminiAiTutorService gemini,
                                 GroqAiTutorService groq,
                                 @Value("${app.ai.primary-provider:groq}") String primaryProvider,
                                 @Value("${app.ai.fallback.enabled:true}") boolean fallbackEnabled) {
        this.contentService = contentService;
        this.gemini = gemini;
        this.groq = groq;
        this.primaryProvider = normalizeProvider(primaryProvider);
        this.fallbackEnabled = fallbackEnabled;
    }

    @Override
    public TopicExplanationResponse explainTopic(ExplainTopicRequest request) {
        return execute(provider -> contentService.explainTopic(request, provider));
    }

    @Override
    public List<GeneratedFlashcard> generateFlashcards(GenerateStudyContentRequest request) {
        return execute(provider -> contentService.generateFlashcards(request, provider));
    }

    @Override
    public List<GeneratedQuizQuestion> generateQuestions(GenerateStudyContentRequest request) {
        return execute(provider -> contentService.generateQuestions(request, provider));
    }

    private <T> T execute(ProviderOperation<T> operation) {
        AiProvider primary = primaryProvider.equals("groq") ? groq : gemini;
        AiProvider fallback = primaryProvider.equals("groq") ? gemini : groq;
        String primaryName = capitalize(primaryProvider);
        String fallbackName = capitalize(primaryProvider.equals("groq") ? "gemini" : "groq");
        try {
            T response = operation.generate(primary);
            logger.info("AI generation completed using provider={}", primaryName);
            return response;
        } catch (AiProviderException primaryFailure) {
            if (!fallbackEnabled || primaryProvider.equals("groq") || !primaryFailure.isRetryable()) {
                logger.warn("Primary AI provider failed; provider={}, retryable={}, httpStatus={}",
                        primaryName,
                        primaryFailure.isRetryable(), primaryFailure.getHttpStatus());
                throw primaryFailure;
            }
            logger.warn("Primary AI provider failed temporarily; provider={}, fallback={}, httpStatus={}",
                    primaryName, fallbackName, primaryFailure.getHttpStatus());
            try {
                T response = operation.generate(fallback);
                logger.info("AI generation completed using fallback provider={}", fallbackName);
                return response;
            } catch (AiProviderException | AiProviderUnavailableException fallbackFailure) {
                logger.warn("AI fallback provider failed; provider={}, errorType={}, httpStatus={}",
                        fallbackName,
                        fallbackFailure.getClass().getSimpleName(),
                        fallbackFailure instanceof AiProviderException providerException
                                ? providerException.getHttpStatus() : null);
                if (fallbackFailure instanceof AiProviderException providerException
                        && providerException.isTimeout()) {
                    throw providerException;
                }
                throw new AiProviderException("No fue posible obtener una respuesta del proveedor de IA");
            }
        }
    }

    private String normalizeProvider(String provider) {
        if (provider == null || (!provider.equalsIgnoreCase("groq") && !provider.equalsIgnoreCase("gemini"))) {
            throw new IllegalArgumentException("app.ai.primary-provider debe ser 'groq' o 'gemini'");
        }
        return provider.toLowerCase(java.util.Locale.ROOT);
    }

    private String capitalize(String provider) {
        return Character.toUpperCase(provider.charAt(0)) + provider.substring(1);
    }

    @FunctionalInterface
    private interface ProviderOperation<T> {
        T generate(AiProvider provider);
    }
}

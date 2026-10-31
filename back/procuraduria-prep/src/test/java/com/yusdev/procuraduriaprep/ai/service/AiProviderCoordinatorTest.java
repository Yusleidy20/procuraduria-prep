package com.yusdev.procuraduriaprep.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yusdev.procuraduriaprep.ai.dto.ExplainTopicRequest;
import com.yusdev.procuraduriaprep.ai.dto.TopicExplanationResponse;

@ExtendWith(MockitoExtension.class)
class AiProviderCoordinatorTest {

    private static final String PROMPT = "safe prompt";
    private static final Map<String, Object> SCHEMA = Map.of();
    private final ExplainTopicRequest request = new ExplainTopicRequest("Tema", null, null, null);
    private final TopicExplanationResponse response = new TopicExplanationResponse("Explicación", "Puntos clave");

    @Mock
    private AiTutorContentService contentService;

    @Mock
    private GeminiAiTutorService gemini;

    @Mock
    private GroqAiTutorService groq;

    @BeforeEach
    void delegatePromptToSelectedProvider() {
        lenient().when(contentService.explainTopic(eq(request), any(AiProvider.class))).thenAnswer(invocation -> {
            AiProvider provider = invocation.getArgument(1);
            provider.generate(PROMPT, SCHEMA);
            return response;
        });
    }

    @Test
    void usesGroqAsPrimaryAndDoesNotCallGemini() {
        when(groq.generate(PROMPT, SCHEMA)).thenReturn("{}");

        assertEquals(response, coordinator("groq", true).explainTopic(request));

        verify(groq).generate(PROMPT, SCHEMA);
        verify(gemini, never()).generate(any(), any());
    }

    @Test
    void fallsBackAfterGemini503() {
        when(gemini.generate(PROMPT, SCHEMA))
                .thenThrow(new AiProviderException("safe error", "Gemini", true, 503));
        when(groq.generate(PROMPT, SCHEMA)).thenReturn("{}");

        assertEquals(response, coordinator("gemini", true).explainTopic(request));
        verify(groq).generate(PROMPT, SCHEMA);
    }

    @Test
    void fallsBackAfterGeminiTimeout() {
        when(gemini.generate(PROMPT, SCHEMA))
                .thenThrow(new AiProviderException("timeout", "Gemini", true, null));
        when(groq.generate(PROMPT, SCHEMA)).thenReturn("{}");

        assertEquals(response, coordinator("gemini", true).explainTopic(request));
        verify(groq).generate(PROMPT, SCHEMA);
    }

    @Test
    void doesNotFallBackAfterGemini400() {
        when(gemini.generate(PROMPT, SCHEMA))
                .thenThrow(new AiProviderException("safe error", "Gemini", false, 400));

        AiProviderException exception = assertThrows(AiProviderException.class,
                () -> coordinator("gemini", true).explainTopic(request));

        assertEquals(400, exception.getHttpStatus());
        verify(groq, never()).generate(any(), any());
    }

    @Test
    void returnsGenericErrorWhenGeminiAndGroqBothFail() {
        when(gemini.generate(PROMPT, SCHEMA))
                .thenThrow(new AiProviderException("provider detail", "Gemini", true, 503));
        when(groq.generate(PROMPT, SCHEMA))
                .thenThrow(new AiProviderException("other provider detail", "Groq", true, 503));

        AiProviderException exception = assertThrows(AiProviderException.class,
                () -> coordinator("gemini", true).explainTopic(request));

        assertEquals("No fue posible obtener una respuesta del proveedor de IA", exception.getMessage());
        verify(gemini).generate(PROMPT, SCHEMA);
        verify(groq).generate(PROMPT, SCHEMA);
    }

    @Test
    void doesNotCallGroqWhenFallbackDisabled() {
        when(gemini.generate(PROMPT, SCHEMA))
                .thenThrow(new AiProviderException("safe error", "Gemini", true, 503));

        assertThrows(AiProviderException.class, () -> coordinator("gemini", false).explainTopic(request));

        verify(groq, never()).generate(any(), any());
    }

    @Test
    void doesNotFallbackOnOutputValidationFailure() {
        when(contentService.explainTopic(eq(request), any(AiProvider.class)))
                .thenThrow(new AiOutputValidationException(java.util.List.of("invalid generated output")));

        assertThrows(AiOutputValidationException.class, () -> coordinator("groq", true).explainTopic(request));

        verify(gemini, never()).generate(any(), any());
        verify(groq, never()).generate(any(), any());
    }

    @Test
    void canSelectGeminiAsPrimaryFromConfiguration() {
        when(gemini.generate(PROMPT, SCHEMA)).thenReturn("{}");

        assertEquals(response, coordinator("gemini", true).explainTopic(request));

        verify(gemini).generate(PROMPT, SCHEMA);
        verify(groq, never()).generate(any(), any());
    }

    @Test
    void doesNotCallGeminiWhenGroqPrimaryFails() {
        when(groq.generate(PROMPT, SCHEMA))
                .thenThrow(new AiProviderException("Groq timeout", "Groq", true, null));

        AiProviderException exception = assertThrows(AiProviderException.class,
                () -> coordinator("groq", true).explainTopic(request));

        assertEquals("Groq timeout", exception.getMessage());
        verify(gemini, never()).generate(any(), any());
    }

    private AiProviderCoordinator coordinator(String primaryProvider, boolean fallbackEnabled) {
        return new AiProviderCoordinator(contentService, gemini, groq, primaryProvider, fallbackEnabled);
    }
}

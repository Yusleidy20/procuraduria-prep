package com.yusdev.procuraduriaprep.ai.dto;

import java.util.UUID;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExplainTopicRequest(
        @NotBlank @Size(max = 2000) String topicName,
        @Size(max = 12000) String sourceContent,
        @Size(max = 80) String learnerLevel,
        UUID sourceDocumentId,
        @Size(max = 5) List<@Size(max = 500) String> previousQuestions,
        @Size(max = 10) List<StudyConversationContextMessage> conversationHistory) {

    public ExplainTopicRequest(String topicName, String sourceContent, String learnerLevel, UUID sourceDocumentId) {
        this(topicName, sourceContent, learnerLevel, sourceDocumentId, List.of(), List.of());
    }

    public ExplainTopicRequest(String topicName, String sourceContent, String learnerLevel, UUID sourceDocumentId,
                               List<String> previousQuestions) {
        this(topicName, sourceContent, learnerLevel, sourceDocumentId, previousQuestions, List.of());
    }

    public ExplainTopicRequest {
        previousQuestions = previousQuestions == null ? List.of() : List.copyOf(previousQuestions);
        conversationHistory = conversationHistory == null ? List.of() : List.copyOf(conversationHistory);
    }
}

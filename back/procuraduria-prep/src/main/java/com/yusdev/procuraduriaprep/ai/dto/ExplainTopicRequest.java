package com.yusdev.procuraduriaprep.ai.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExplainTopicRequest(
        @NotBlank @Size(max = 160) String topicName,
        @Size(max = 12000) String sourceContent,
        @Size(max = 80) String learnerLevel,
        UUID sourceDocumentId) {
}

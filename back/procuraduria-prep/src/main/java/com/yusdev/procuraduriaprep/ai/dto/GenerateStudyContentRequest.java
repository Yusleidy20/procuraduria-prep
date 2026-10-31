package com.yusdev.procuraduriaprep.ai.dto;

import java.util.UUID;

import com.yusdev.procuraduriaprep.study.entity.DifficultyLevel;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GenerateStudyContentRequest(
        @NotNull UUID topicId,
        @NotBlank @Size(max = 160) String topicName,
        @Size(max = 12000) String sourceContent,
        UUID sourceDocumentId,
        @NotNull DifficultyLevel difficulty,
        @Min(1) @Max(20) int quantity) {
}

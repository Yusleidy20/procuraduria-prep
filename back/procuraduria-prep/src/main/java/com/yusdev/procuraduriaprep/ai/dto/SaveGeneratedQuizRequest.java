package com.yusdev.procuraduriaprep.ai.dto;

import java.util.List;
import java.util.UUID;

import com.yusdev.procuraduriaprep.study.entity.DifficultyLevel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveGeneratedQuizRequest(
        @NotNull UUID topicId,
        UUID sourceDocumentId,
        @NotBlank @Size(max = 180) String name,
        @Size(max = 2000) String description,
        @NotNull DifficultyLevel difficulty,
        @NotEmpty @Size(max = 20) List<@Valid GeneratedQuizQuestion> questions) {
}

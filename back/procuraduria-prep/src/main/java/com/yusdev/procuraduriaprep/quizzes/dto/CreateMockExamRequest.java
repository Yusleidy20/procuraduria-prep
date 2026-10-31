package com.yusdev.procuraduriaprep.quizzes.dto;

import java.util.List;
import java.util.UUID;

import com.yusdev.procuraduriaprep.study.entity.DifficultyLevel;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMockExamRequest(
        @NotBlank @Size(max = 180) String name,
        @Size(max = 2000) String description,
        @NotEmpty @Size(min = 2, max = 20) List<@NotNull UUID> topicIds,
        @NotNull DifficultyLevel difficulty,
        @Min(1) @Max(100) int questionCount,
        @Min(1) @Max(360) int durationMinutes) {
}

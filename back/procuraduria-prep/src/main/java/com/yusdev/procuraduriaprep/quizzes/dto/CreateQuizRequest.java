package com.yusdev.procuraduriaprep.quizzes.dto;

import java.util.List;

import com.yusdev.procuraduriaprep.study.entity.DifficultyLevel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreateQuizRequest(
        @NotBlank @Size(max = 180) String name,
        @Size(max = 2000) String description,
        @NotNull DifficultyLevel difficulty,
        @Min(1) @Max(360) Integer durationMinutes,
        @NotEmpty @Size(max = 100) List<@Valid CreateQuizQuestionRequest> questions) {
}

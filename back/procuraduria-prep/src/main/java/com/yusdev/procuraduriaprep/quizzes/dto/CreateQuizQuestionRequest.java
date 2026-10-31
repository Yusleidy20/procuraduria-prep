package com.yusdev.procuraduriaprep.quizzes.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateQuizQuestionRequest(
		@NotBlank @Size(max = 3000) String statement,
		@NotEmpty @Size(min = 2, max = 6) List<@NotBlank @Size(max = 500) String> options,
		@Min(0) int correctOptionIndex,
		@Size(max = 3000) String explanation,
		UUID sourceDocumentId) {
}

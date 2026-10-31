package com.yusdev.procuraduriaprep.quizzes.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RecordQuizAnswerRequest(
		@NotNull UUID questionId,
		@Min(0) int selectedOptionIndex) {
}

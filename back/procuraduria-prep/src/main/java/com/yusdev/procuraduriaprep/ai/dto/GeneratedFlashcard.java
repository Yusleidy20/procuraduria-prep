package com.yusdev.procuraduriaprep.ai.dto;

import com.yusdev.procuraduriaprep.study.entity.DifficultyLevel;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GeneratedFlashcard(
		@NotBlank @Size(max = 2000) String question,
		@NotBlank @Size(max = 5000) String answer,
		@Size(max = 3000) String explanation,
		@NotNull DifficultyLevel difficulty,
		List<DocumentSourceReference> sources) {

	public GeneratedFlashcard(String question, String answer, String explanation, DifficultyLevel difficulty) {
		this(question, answer, explanation, difficulty, List.of());
	}

	public GeneratedFlashcard {
		sources = sources == null ? List.of() : List.copyOf(sources);
	}
}
